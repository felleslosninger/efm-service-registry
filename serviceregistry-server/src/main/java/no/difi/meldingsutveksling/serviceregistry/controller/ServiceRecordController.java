package no.difi.meldingsutveksling.serviceregistry.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.domain.FiksIoIdentifier;
import no.difi.meldingsutveksling.domain.Iso6523;
import no.difi.meldingsutveksling.domain.PartnerIdentifier;
import no.difi.meldingsutveksling.domain.PersonIdentifier;
import no.difi.meldingsutveksling.serviceregistry.CertificateNotFoundException;
import no.difi.meldingsutveksling.serviceregistry.SRRequestScope;
import no.difi.meldingsutveksling.serviceregistry.domain.Process;
import no.difi.meldingsutveksling.serviceregistry.domain.*;
import no.difi.meldingsutveksling.serviceregistry.exceptions.EntityNotFoundException;
import no.difi.meldingsutveksling.serviceregistry.exceptions.ReceiverProcessNotFoundException;
import no.difi.meldingsutveksling.serviceregistry.exceptions.SecurityLevelNotFoundException;
import no.difi.meldingsutveksling.serviceregistry.exceptions.ServiceRegistryException;
import no.difi.meldingsutveksling.serviceregistry.freg.exception.FregGatewayException;
import no.difi.meldingsutveksling.serviceregistry.krr.KontaktInfoException;
import no.difi.meldingsutveksling.serviceregistry.record.ServiceRecord;
import no.difi.meldingsutveksling.serviceregistry.record.ServiceRecordService;
import no.difi.meldingsutveksling.serviceregistry.security.EntitySignerException;
import no.difi.meldingsutveksling.serviceregistry.security.PayloadSigner;
import no.difi.meldingsutveksling.serviceregistry.service.AuthenticationService;
import no.difi.meldingsutveksling.serviceregistry.service.EntityService;
import no.difi.meldingsutveksling.serviceregistry.service.ProcessService;
import no.difi.meldingsutveksling.serviceregistry.service.brreg.BrregNotFoundException;
import no.difi.meldingsutveksling.serviceregistry.svarut.SvarUtClientException;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

import static no.difi.meldingsutveksling.serviceregistry.logging.SRMarkerFactory.markerFrom;

@RestController
@RequiredArgsConstructor
@Slf4j
public class ServiceRecordController {

    private final ServiceRecordService serviceRecordService;
    private final ProcessService processService;
    private final AuthenticationService authenticationService;
    private final EntityService entityService;
    private final PayloadSigner payloadSigner;
    private final SRRequestScope requestScope;
    private final ObjectMapper objectMapper;

    @InitBinder
    protected void initBinders(WebDataBinder binder) {
        binder.registerCustomEditor(Notification.class, new NotificationEditor());
    }

    /**
     * Used to retrieve information needed to send a message within the provided process
     * to an entity with the provided identifier.
     *
     * @param identifier        specifies the target entity.
     * @param processIdentifier specifies the target process.
     * @param auth              provides the authentication object.
     * @param request           is the servlet request.
     * @return JSON object with information needed to send a message.
     */
    @GetMapping(value = "/identifier/{identifier}/process/{processIdentifier}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<?> entity(@PathVariable("identifier") PartnerIdentifier identifier,
                                    @PathVariable("processIdentifier") String processIdentifier,
                                    @RequestParam(name = "securityLevel", required = false) Integer securityLevel,
                                    @RequestParam(name = "conversationId", required = false) String conversationId,
                                    @RequestParam(name = "print", defaultValue = "true") boolean print,
                                    Authentication auth,
                                    HttpServletRequest request)
            throws SecurityLevelNotFoundException, CertificateNotFoundException, KontaktInfoException,

            BrregNotFoundException, SvarUtClientException, ReceiverProcessNotFoundException, FregGatewayException {
        MDC.put("identifier", identifier instanceof PersonIdentifier ? DigestUtils.sha256Hex(identifier.toString()) : identifier.toString());
        Iso6523 clientId = authenticationService.getAuthorizedClientIdentifier(auth, request);
        fillRequestScope(identifier, conversationId, clientId, authenticationService.getToken(auth));

        EntityInfo entityInfo = entityService.getEntityInfo(identifier)
            .orElseThrow(() -> new EntityNotFoundException(identifier.getIdentifier()));
        Entity entity = new Entity();
        entity.setInfoRecord(entityInfo);

        if (entityInfo instanceof FiksIoInfo) {
            ServiceRecord fiksIoRecord = serviceRecordService.createFiksIoServiceRecord(entityInfo, processIdentifier)
                .orElseThrow(() -> new ReceiverProcessNotFoundException(identifier, processIdentifier));
            entity.getServiceRecords().add(fiksIoRecord);
            return ResponseEntity.ok(entity);
        }


        Process process = processService.findByIdentifier(processIdentifier).orElseThrow(() -> new ReceiverProcessNotFoundException(identifier, processIdentifier));
        if (ProcessCategory.DIGITALPOST == process.getCategory() && identifier instanceof PersonIdentifier) {
            entity.getServiceRecords().addAll(serviceRecordService.createDigitalpostServiceRecords(identifier.cast(PersonIdentifier.class), clientId, print, process));
        }
        if (ProcessCategory.ARKIVMELDING == process.getCategory()) {
            ServiceRecord record = serviceRecordService.createArkivmeldingServiceRecord(identifier.cast(Iso6523.class), process, securityLevel)
                .orElseThrow(() -> new ReceiverProcessNotFoundException(identifier, processIdentifier));
            entity.getServiceRecords().add(record);
        }
        if (ProcessCategory.EINNSYN == process.getCategory()) {
            ServiceRecord record = serviceRecordService.createServiceRecord(identifier.cast(Iso6523.class), process)
                .orElseThrow(() -> new ReceiverProcessNotFoundException(identifier, processIdentifier));
            entity.getServiceRecords().add(record);
        }
        if (ProcessCategory.AVTALT == process.getCategory()) {
            ServiceRecord record = serviceRecordService.createServiceRecord(identifier.cast(Iso6523.class), process)
                .orElseThrow(() -> new ReceiverProcessNotFoundException(identifier, processIdentifier));
            entity.getServiceRecords().add(record);
        }
        if (entity.getServiceRecords().isEmpty()) {
            throw new ReceiverProcessNotFoundException(identifier, processIdentifier);
        }

        return ResponseEntity.ok(entity);
    }

    /**
     * Used to retrieve information needed to send a message to an entity (organization or a person) having the provided
     * identifier
     *
     * @param identifier of the organization/person to receive a message
     * @return JSON object with information needed to send a message
     */
    @GetMapping(value = "/identifier/{identifier}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @SuppressWarnings("squid:S2583")
    public ResponseEntity<?> entity(
        @PathVariable("identifier") PartnerIdentifier identifier,
        @RequestParam(name = "securityLevel", required = false) Integer securityLevel,
        @RequestParam(name = "conversationId", required = false) String conversationId,
        @RequestParam(name = "print", defaultValue = "true") boolean print,
        Authentication auth,
        HttpServletRequest request)

            throws SecurityLevelNotFoundException, CertificateNotFoundException, KontaktInfoException, BrregNotFoundException, SvarUtClientException, FregGatewayException {
        MDC.put("identifier", identifier instanceof PersonIdentifier ? DigestUtils.sha256Hex(identifier.toString()) : identifier.toString());
        Iso6523 clientIdentifier = authenticationService.getAuthorizedClientIdentifier(auth, request);
        fillRequestScope(identifier, conversationId, clientIdentifier, authenticationService.getToken(auth));

        Entity entity = new Entity();
        EntityInfo entityInfo = entityService.getEntityInfo(identifier)
            .orElseThrow(() -> new EntityNotFoundException(identifier.toString()));
        entity.setInfoRecord(entityInfo);

        if (identifier instanceof FiksIoIdentifier) {
            return new ResponseEntity<>(entity, HttpStatus.OK);
        }

        if (identifier instanceof PersonIdentifier) {
            entity.getServiceRecords().addAll(serviceRecordService.createDigitalpostServiceRecords(identifier.cast(PersonIdentifier.class), clientIdentifier, print));
        } else if (identifier instanceof Iso6523){
            entity.getServiceRecords().addAll(serviceRecordService.createArkivmeldingServiceRecords(identifier.cast(Iso6523.class), securityLevel));
            entity.getServiceRecords().addAll(serviceRecordService.createEinnsynServiceRecords(identifier.cast(Iso6523.class)));
            entity.getServiceRecords().addAll(serviceRecordService.createAvtaltServiceRecords(identifier.cast(Iso6523.class)));
        }
        return new ResponseEntity<>(entity, HttpStatus.OK);
    }

    private void fillRequestScope(PartnerIdentifier identifier, String conversationId, Iso6523 clientId, Jwt token) {
        requestScope.setConversationId(conversationId);
        requestScope.setIdentifier(identifier);
        requestScope.setClientId(clientId);
        requestScope.setToken(token);
    }

    private ResponseEntity<?> notFoundResponse(String logMessage) {
        log.error(markerFrom(requestScope), logMessage);
        return ResponseEntity.notFound().build();
    }

    @GetMapping(value = "/identifier/{identifier}", produces = "application/jose")
    @ResponseBody
    public ResponseEntity<?> signed(
            @PathVariable("identifier") PartnerIdentifier identifier,
            @RequestParam(name = "securityLevel", required = false) Integer securityLevel,
            @RequestParam(name = "conversationId", required = false) String conversationId,
            @RequestParam(name = "print", defaultValue = "true") boolean print,
            Authentication auth,
            HttpServletRequest request)
            throws EntitySignerException, SecurityLevelNotFoundException, KontaktInfoException,
            CertificateNotFoundException, BrregNotFoundException, SvarUtClientException, FregGatewayException {
        return signEntity(entity(identifier, securityLevel, conversationId, print, auth, request));
    }

    @GetMapping(value = "/identifier/{identifier}/process/{processIdentifier}", produces = "application/jose")
    @ResponseBody
    public ResponseEntity<?> signed(@PathVariable("identifier") PartnerIdentifier identifier,
                                    @PathVariable("processIdentifier") String processIdentifier,
                                    @RequestParam(name = "securityLevel", required = false) Integer securityLevel,
                                    @RequestParam(name = "conversationId", required = false) String conversationId,
                                    @RequestParam(name = "print", defaultValue = "true") boolean print,
                                    Authentication auth,
                                    HttpServletRequest request)
            throws SecurityLevelNotFoundException, KontaktInfoException, CertificateNotFoundException,
            BrregNotFoundException, SvarUtClientException, EntitySignerException, ReceiverProcessNotFoundException, FregGatewayException {
        return signEntity(entity(identifier, processIdentifier, securityLevel, conversationId, print, auth, request));
    }

    @GetMapping(value = "/info/{identifier}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<?> info(@PathVariable("identifier") PartnerIdentifier identifier) {
        Entity entity = new Entity();
        Optional<EntityInfo> entityInfo = entityService.getEntityInfo(identifier);
        if (entityInfo.isEmpty()) {
            return notFoundResponse(String.format("Entity with identifier '%s' not found.", identifier));
        }
        entity.setInfoRecord(entityInfo.get());
        return ResponseEntity.ok(entity);
    }

    @GetMapping(value = "/info/{identifier}", produces = "application/jose")
    @ResponseBody
    public ResponseEntity<?> signed(@PathVariable("identifier") PartnerIdentifier identifier) throws EntitySignerException {
        return signEntity(info(identifier));
    }

    private ResponseEntity<?> signEntity(ResponseEntity<?> entity) throws EntitySignerException {
        if (entity.getStatusCode() != HttpStatus.OK) {
            log.warn("Entity status code is {}, skipping signing", entity.getStatusCode());
            return entity;
        }
        String json;
        try {
            json = objectMapper.writeValueAsString(entity.getBody());
        } catch (JsonProcessingException e) {
            log.error(markerFrom(requestScope), "Failed to convert entity to json", e);
            throw new ServiceRegistryException(e);
        }

        return ResponseEntity.ok(payloadSigner.sign(json));
    }
}
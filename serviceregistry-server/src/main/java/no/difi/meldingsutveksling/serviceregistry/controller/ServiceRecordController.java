package no.difi.meldingsutveksling.serviceregistry.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.serviceregistry.CertificateNotFoundException;
import no.difi.meldingsutveksling.serviceregistry.SRRequestScope;
import no.difi.meldingsutveksling.serviceregistry.exceptions.ServiceRegistryException;
import no.difi.meldingsutveksling.serviceregistry.domain.Process;
import no.difi.meldingsutveksling.serviceregistry.domain.*;
import no.difi.meldingsutveksling.serviceregistry.exceptions.EntityNotFoundException;
import no.difi.meldingsutveksling.serviceregistry.exceptions.ReceiverProcessNotFoundException;
import no.difi.meldingsutveksling.serviceregistry.exceptions.SecurityLevelNotFoundException;
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
import no.difi.move.common.IdentifierHasher;
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

import static no.difi.meldingsutveksling.serviceregistry.businesslogic.ServiceRecordPredicates.shouldCreateServiceRecordForCitizen;
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
    public ResponseEntity<?> entity(@PathVariable("identifier") String identifier,
                                    @PathVariable("processIdentifier") String processIdentifier,
                                    @RequestParam(name = "securityLevel", required = false) Integer securityLevel,
                                    @RequestParam(name = "conversationId", required = false) String conversationId,
                                    @RequestParam(name = "print", defaultValue = "true") boolean print,
                                    Authentication auth,
                                    HttpServletRequest request)
        throws SecurityLevelNotFoundException, CertificateNotFoundException, KontaktInfoException,
        BrregNotFoundException, SvarUtClientException, ReceiverProcessNotFoundException {
        MDC.put("identifier", Strings.isNullOrEmpty(identifier) ? identifier : IdentifierHasher.hashIfPersonnr(identifier));
        String clientId = authenticationService.getAuthorizedClientIdentifier(auth, request);
        fillRequestScope(identifier, conversationId, clientId, authenticationService.getToken(auth));

        EntityInfo entityInfo = entityService.getEntityInfo(identifier)
            .orElseThrow(() -> new EntityNotFoundException(identifier));
        Entity entity = new Entity();
        entity.setInfoRecord(entityInfo);

        if (entityInfo instanceof FiksIoInfo) {
            ServiceRecord fiksIoRecord = serviceRecordService.createFiksIoServiceRecord(entityInfo, processIdentifier)
                .orElseThrow(() -> new ReceiverProcessNotFoundException(identifier, processIdentifier));
            entity.getServiceRecords().add(fiksIoRecord);
            return ResponseEntity.ok(entity);
        }

        Process process = processService.findByIdentifier(processIdentifier).orElseThrow(() -> new ReceiverProcessNotFoundException(identifier, processIdentifier));
        if (ProcessCategory.DIGITALPOST == process.getCategory() && shouldCreateServiceRecordForCitizen().test(entityInfo)) {
            entity.getServiceRecords().addAll(serviceRecordService.createDigitalpostServiceRecords(identifier, clientId, print, process));
        }
        if (ProcessCategory.ARKIVMELDING == process.getCategory()) {
            ServiceRecord record = serviceRecordService.createArkivmeldingServiceRecord(entityInfo, process, securityLevel)
                .orElseThrow(() -> new ReceiverProcessNotFoundException(identifier, processIdentifier));
            entity.getServiceRecords().add(record);
        }
        if (ProcessCategory.EINNSYN == process.getCategory()) {
            ServiceRecord record = serviceRecordService.createServiceRecord(entityInfo, process, securityLevel)
                .orElseThrow(() -> new ReceiverProcessNotFoundException(identifier, processIdentifier));
            entity.getServiceRecords().add(record);
        }
        if (ProcessCategory.AVTALT == process.getCategory()) {
            ServiceRecord record = serviceRecordService.createServiceRecord(entityInfo, process, securityLevel)
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
        @PathVariable("identifier") String identifier,
        @RequestParam(name = "securityLevel", required = false) Integer securityLevel,
        @RequestParam(name = "conversationId", required = false) String conversationId,
        @RequestParam(name = "print", defaultValue = "true") boolean print,
        Authentication auth,
        HttpServletRequest request)
        throws SecurityLevelNotFoundException, CertificateNotFoundException, KontaktInfoException, BrregNotFoundException, SvarUtClientException {
        MDC.put("identifier", Strings.isNullOrEmpty(identifier) ? identifier : IdentifierHasher.hashIfPersonnr(identifier));
        String clientOrgnr = authenticationService.getAuthorizedClientIdentifier(auth, request);
        fillRequestScope(identifier, conversationId, clientOrgnr, authenticationService.getToken(auth));

        Entity entity = new Entity();
        EntityInfo entityInfo = entityService.getEntityInfo(identifier)
            .orElseThrow(() -> new EntityNotFoundException(identifier));
        entity.setInfoRecord(entityInfo);

        if (entityInfo instanceof FiksIoInfo) {
            return new ResponseEntity<>(entity, HttpStatus.OK);
        }

        if (shouldCreateServiceRecordForCitizen().test(entityInfo)) {
            entity.getServiceRecords().addAll(serviceRecordService.createDigitalpostServiceRecords(identifier, clientOrgnr, print));
        } else {
            entity.getServiceRecords().addAll(serviceRecordService.createArkivmeldingServiceRecords(entityInfo, securityLevel));
            entity.getServiceRecords().addAll(serviceRecordService.createEinnsynServiceRecords(entityInfo, securityLevel));
            entity.getServiceRecords().addAll(serviceRecordService.createAvtaltServiceRecords(identifier));
        }
        return new ResponseEntity<>(entity, HttpStatus.OK);
    }

    private void fillRequestScope(String identifier, String conversationId, String clientId, Jwt token) {
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
        @PathVariable("identifier") String identifier,
        @RequestParam(name = "securityLevel", required = false) Integer securityLevel,
        @RequestParam(name = "conversationId", required = false) String conversationId,
        @RequestParam(name = "print", defaultValue = "true") boolean print,
        Authentication auth,
        HttpServletRequest request)
        throws EntitySignerException, SecurityLevelNotFoundException, KontaktInfoException,
        CertificateNotFoundException, BrregNotFoundException, SvarUtClientException {
        return signEntity(entity(identifier, securityLevel, conversationId, print, auth, request));
    }

    @GetMapping(value = "/identifier/{identifier}/process/{processIdentifier}", produces = "application/jose")
    @ResponseBody
    public ResponseEntity<?> signed(@PathVariable("identifier") String identifier,
                                    @PathVariable("processIdentifier") String processIdentifier,
                                    @RequestParam(name = "securityLevel", required = false) Integer securityLevel,
                                    @RequestParam(name = "conversationId", required = false) String conversationId,
                                    @RequestParam(name = "print", defaultValue = "true") boolean print,
                                    Authentication auth,
                                    HttpServletRequest request)
        throws SecurityLevelNotFoundException, KontaktInfoException, CertificateNotFoundException,
        BrregNotFoundException, SvarUtClientException, EntitySignerException, ReceiverProcessNotFoundException {
        return signEntity(entity(identifier, processIdentifier, securityLevel, conversationId, print, auth, request));
    }

    @GetMapping(value = "/info/{identifier}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<?> info(@PathVariable("identifier") String identifier) {
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
    public ResponseEntity<?> signed(@PathVariable("identifier") String identifier) throws EntitySignerException {
        return signEntity(info(identifier));
    }

    private ResponseEntity<?> signEntity(ResponseEntity<?> entity) throws EntitySignerException {
        if (entity.getStatusCode() != HttpStatus.OK) {
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

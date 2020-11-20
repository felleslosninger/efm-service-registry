package no.difi.meldingsutveksling.serviceregistry.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.serviceregistry.CertificateNotFoundException;
import no.difi.meldingsutveksling.serviceregistry.ErrorResponse;
import no.difi.meldingsutveksling.serviceregistry.SRRequestScope;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryException;
import no.difi.meldingsutveksling.serviceregistry.domain.Process;
import no.difi.meldingsutveksling.serviceregistry.domain.*;
import no.difi.meldingsutveksling.serviceregistry.exceptions.EntityNotFoundException;
import no.difi.meldingsutveksling.serviceregistry.exceptions.ProcessNotFoundException;
import no.difi.meldingsutveksling.serviceregistry.exceptions.SecurityLevelNotFoundException;
import no.difi.meldingsutveksling.serviceregistry.krr.DsfLookupException;
import no.difi.meldingsutveksling.serviceregistry.krr.KRRClientException;
import no.difi.meldingsutveksling.serviceregistry.record.ServiceRecord;
import no.difi.meldingsutveksling.serviceregistry.record.ServiceRecordService;
import no.difi.meldingsutveksling.serviceregistry.security.EntitySignerException;
import no.difi.meldingsutveksling.serviceregistry.security.PayloadSigner;
import no.difi.meldingsutveksling.serviceregistry.service.AuthenticationService;
import no.difi.meldingsutveksling.serviceregistry.service.EntityService;
import no.difi.meldingsutveksling.serviceregistry.service.ProcessService;
import no.difi.meldingsutveksling.serviceregistry.service.brreg.BrregNotFoundException;
import no.difi.meldingsutveksling.serviceregistry.svarut.SvarUtClientException;
import org.jboss.logging.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

import static no.difi.meldingsutveksling.serviceregistry.businesslogic.ServiceRecordPredicates.shouldCreateServiceRecordForCitizen;
import static no.difi.meldingsutveksling.serviceregistry.logging.SRMarkerFactory.markerFrom;

@RestController
@Slf4j
public class ServiceRecordController {

    private final ServiceRecordService serviceRecordService;
    private final ProcessService processService;
    private final AuthenticationService authenticationService;
    private final EntityService entityService;
    private final PayloadSigner payloadSigner;
    private final SRRequestScope requestScope;

    public ServiceRecordController(ServiceRecordService serviceRecordService,
                                   EntityService entityService,
                                   PayloadSigner payloadSigner,
                                   ProcessService processService,
                                   AuthenticationService authenticationService,
                                   SRRequestScope requestScope) {
        this.entityService = entityService;
        this.serviceRecordService = serviceRecordService;
        this.payloadSigner = payloadSigner;
        this.processService = processService;
        this.authenticationService = authenticationService;
        this.requestScope = requestScope;
    }

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
                                 Authentication auth,
                                 HttpServletRequest request)
            throws SecurityLevelNotFoundException, KRRClientException, CertificateNotFoundException,
            DsfLookupException, BrregNotFoundException, SvarUtClientException, ProcessNotFoundException {
        MDC.put("entity", identifier);
        String clientId = authenticationService.getAuthorizedClientIdentifier(auth, request);
        fillRequestScope(identifier, conversationId, clientId, authenticationService.getToken(auth));
        Optional<EntityInfo> optionalEntityInfo = entityService.getEntityInfo(identifier);
        if (!optionalEntityInfo.isPresent()) {
            return notFoundResponse(String.format("Entity with identifier '%s' not found.", identifier));
        }
        Process process = processService.findByIdentifier(processIdentifier)
                .orElseThrow(() -> new ProcessNotFoundException(processIdentifier));
        Entity entity = new Entity();
        EntityInfo entityInfo = optionalEntityInfo.get();
        entity.setInfoRecord(entityInfo);
        ServiceRecord serviceRecord = null;
        if (ProcessCategory.DIGITALPOST == process.getCategory() && shouldCreateServiceRecordForCitizen().test(entityInfo)) {
            if (clientId == null) {
                return errorResponse(HttpStatus.UNAUTHORIZED, "No authentication provided.");
            }
            entity.getServiceRecords().addAll(serviceRecordService.createDigitalpostServiceRecords(identifier, clientId));
        }
        if (ProcessCategory.ARKIVMELDING == process.getCategory()) {
            Optional<ServiceRecord> arkivmeldingServiceRecord = serviceRecordService.createArkivmeldingServiceRecord(entityInfo, process, securityLevel);
            if (!arkivmeldingServiceRecord.isPresent()) {
                return notFoundResponse(String.format("Arkivmelding process '%s' not found for receiver '%s'.", process.getIdentifier(), identifier));
            }
            serviceRecord = arkivmeldingServiceRecord.get();
        }
        if (ProcessCategory.EINNSYN == process.getCategory()) {
            Optional<ServiceRecord> dpeServiceRecord = serviceRecordService.createServiceRecord(entityInfo, process, securityLevel);
            if (!dpeServiceRecord.isPresent()) {
                return notFoundResponse(String.format("eInnsyn process '%s' not found for receiver '%s'.", process.getIdentifier(), identifier));
            }
            serviceRecord = dpeServiceRecord.get();
        }
        if(ProcessCategory.AVTALT == process.getCategory()) {
            Optional<ServiceRecord> avtaltDpoServiceRecord = serviceRecordService.createServiceRecord(entityInfo, process, securityLevel);
            if(!avtaltDpoServiceRecord.isPresent()) {
                return notFoundResponse(String.format("Avtalt process '%s' not found for receiver '%s'.", process.getIdentifier(), identifier));
            }
            serviceRecord = avtaltDpoServiceRecord.get();
        }
        entity.getServiceRecords().add(serviceRecord);
        return new ResponseEntity<>(entity, HttpStatus.OK);
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
            Authentication auth,
            HttpServletRequest request)
            throws SecurityLevelNotFoundException, KRRClientException, CertificateNotFoundException, DsfLookupException, BrregNotFoundException, SvarUtClientException {
        MDC.put("identifier", identifier);
        String clientOrgnr = authenticationService.getAuthorizedClientIdentifier(auth, request);
        fillRequestScope(identifier, conversationId, clientOrgnr, authenticationService.getToken(auth));
        Entity entity = new Entity();
        EntityInfo entityInfo = entityService.getEntityInfo(identifier)
                .orElseThrow(() -> new EntityNotFoundException(identifier));
        entity.setInfoRecord(entityInfo);
        if (shouldCreateServiceRecordForCitizen().test(entityInfo)) {
            if (clientOrgnr == null) {
                return errorResponse(HttpStatus.UNAUTHORIZED, "No authentication provided.");
            }
            entity.getServiceRecords().addAll(serviceRecordService.createDigitalpostServiceRecords(identifier, clientOrgnr));
        } else {
            entity.getServiceRecords().addAll(serviceRecordService.createArkivmeldingServiceRecords(entityInfo, securityLevel));
            entity.getServiceRecords().addAll(serviceRecordService.createEinnsynServiceRecords(entityInfo, securityLevel));
            entity.getServiceRecords().addAll(serviceRecordService.createAvtaltServiceRecords(identifier));
        }
        return new ResponseEntity<>(entity, HttpStatus.OK);
    }

    private void fillRequestScope(String identifier, String conversationId, String clientId, String token) {
        requestScope.setConversationId(conversationId);
        requestScope.setIdentifier(identifier);
        requestScope.setClientId(clientId);
        requestScope.setToken(token);
    }

    private ResponseEntity<ErrorResponse> errorResponse(HttpStatus status, String message) {
        return ResponseEntity.status(status)
                .body(ErrorResponse.builder().errorDescription(message).build());
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
            Authentication auth,
            HttpServletRequest request)
            throws EntitySignerException, SecurityLevelNotFoundException, KRRClientException,
            CertificateNotFoundException, DsfLookupException, BrregNotFoundException, SvarUtClientException {
        return signEntity(entity(identifier, securityLevel, conversationId, auth, request));
    }

    @GetMapping(value = "/identifier/{identifier}/process/{processIdentifier}", produces = "application/jose")
    @ResponseBody
    public ResponseEntity<?> signed(@PathVariable("identifier") String identifier,
                                 @PathVariable("processIdentifier") String processIdentifier,
                                 @RequestParam(name = "securityLevel", required = false) Integer securityLevel,
                                 @RequestParam(name = "conversationId", required = false) String conversationId,
                                 Authentication auth,
                                 HttpServletRequest request)
            throws SecurityLevelNotFoundException, KRRClientException, CertificateNotFoundException,
            DsfLookupException, BrregNotFoundException, SvarUtClientException, EntitySignerException, ProcessNotFoundException {
        return signEntity(entity(identifier, processIdentifier, securityLevel, conversationId, auth, request));
    }

    @GetMapping(value = "/info/{identifier}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<?> info(@PathVariable("identifier") String identifier) {
        Entity entity = new Entity();
        Optional<EntityInfo> entityInfo = entityService.getEntityInfo(identifier);
        if (!entityInfo.isPresent()) {
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
            json = new ObjectMapper().writeValueAsString(entity.getBody());
        } catch (JsonProcessingException e) {
            log.error(markerFrom(requestScope), "Failed to convert entity to json", e);
            throw new ServiceRegistryException(e);
        }

        return ResponseEntity.ok(payloadSigner.sign(json));
    }

}

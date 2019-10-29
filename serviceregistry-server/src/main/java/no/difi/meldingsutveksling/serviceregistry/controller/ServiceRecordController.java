package no.difi.meldingsutveksling.serviceregistry.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.difi.meldingsutveksling.Notification;
import no.difi.meldingsutveksling.serviceregistry.CertificateNotFoundException;
import no.difi.meldingsutveksling.serviceregistry.EntityNotFoundException;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryException;
import no.difi.meldingsutveksling.serviceregistry.exceptions.EndpointUrlNotFound;
import no.difi.meldingsutveksling.serviceregistry.exceptions.SecurityLevelNotFoundException;
import no.difi.meldingsutveksling.serviceregistry.krr.DsfLookupException;
import no.difi.meldingsutveksling.serviceregistry.krr.KRRClientException;
import no.difi.meldingsutveksling.serviceregistry.model.Entity;
import no.difi.meldingsutveksling.serviceregistry.model.EntityInfo;
import no.difi.meldingsutveksling.serviceregistry.model.Process;
import no.difi.meldingsutveksling.serviceregistry.model.ProcessCategory;
import no.difi.meldingsutveksling.serviceregistry.response.ErrorResponse;
import no.difi.meldingsutveksling.serviceregistry.security.EntitySignerException;
import no.difi.meldingsutveksling.serviceregistry.security.PayloadSigner;
import no.difi.meldingsutveksling.serviceregistry.service.AuthenticationService;
import no.difi.meldingsutveksling.serviceregistry.service.EntityService;
import no.difi.meldingsutveksling.serviceregistry.service.ProcessService;
import no.difi.meldingsutveksling.serviceregistry.service.brreg.BrregNotFoundException;
import no.difi.meldingsutveksling.serviceregistry.servicerecord.ServiceRecord;
import no.difi.meldingsutveksling.serviceregistry.servicerecord.ServiceRecordFactory;
import no.difi.meldingsutveksling.serviceregistry.svarut.SvarUtClientException;
import no.difi.meldingsutveksling.serviceregistry.util.SRRequestScope;
import org.jboss.logging.MDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

import static no.difi.meldingsutveksling.serviceregistry.businesslogic.ServiceRecordPredicates.shouldCreateServiceRecordForCitizen;
import static no.difi.meldingsutveksling.serviceregistry.logging.SRMarkerFactory.markerFrom;

@ExposesResourceFor(Entity.class)
@RestController
public class ServiceRecordController {

    private static final Logger log = LoggerFactory.getLogger(ServiceRecordController.class);
    private final ServiceRecordFactory serviceRecordFactory;
    private final ProcessService processService;
    private final AuthenticationService authenticationService;
    private EntityService entityService;
    private PayloadSigner payloadSigner;
    private SRRequestScope requestScope;

    /**
     * @param serviceRecordFactory  for creation of the identifiers respective service record
     * @param entityService         needed to lookup and retrieve organization or citizen information using an identifier number
     */
    public ServiceRecordController(ServiceRecordFactory serviceRecordFactory,
                                   EntityService entityService,
                                   PayloadSigner payloadSigner,
                                   ProcessService processService,
                                   AuthenticationService authenticationService,
                                   SRRequestScope requestScope) {
        this.entityService = entityService;
        this.serviceRecordFactory = serviceRecordFactory;
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
    public ResponseEntity entity(@PathVariable("identifier") String identifier,
                                 @PathVariable("processIdentifier") String processIdentifier,
                                 @RequestParam(name = "securityLevel", required = false) Integer securityLevel,
                                 @RequestParam(name = "conversationId", required = false) String conversationId,
                                 Authentication auth,
                                 HttpServletRequest request)
            throws SecurityLevelNotFoundException, KRRClientException, CertificateNotFoundException,
            DsfLookupException, BrregNotFoundException, SvarUtClientException {
        MDC.put("entity", identifier);
        requestScope.setConversationId(conversationId);
        requestScope.setIdentifier(identifier);
        String clientOrgnr = authenticationService.getOrganizationNumber(auth, request);
        requestScope.setClientId(clientOrgnr);
        Optional<EntityInfo> optionalEntityInfo = entityService.getEntityInfo(identifier);
        if (!optionalEntityInfo.isPresent()) {
            return notFoundResponse(String.format("Entity with identifier '%s' not found.", identifier));
        }
        Optional<Process> optionalProcess = processService.findByIdentifier(processIdentifier);
        if (!optionalProcess.isPresent()) {
            return notFoundResponse(String.format("Process with identifier '%s' not found.", processIdentifier));
        }
        Entity entity = new Entity();
        EntityInfo entityInfo = optionalEntityInfo.get();
        entity.setInfoRecord(entityInfo);
        ServiceRecord serviceRecord = null;
        ProcessCategory processCategory = optionalProcess.get().getCategory();
        if (processCategory.equals(ProcessCategory.DIGITALPOST) && shouldCreateServiceRecordForCitizen().test(entityInfo)) {
            if (clientOrgnr == null) {
                return errorResponse(HttpStatus.UNAUTHORIZED, "No authentication provided.");
            }
            entity.getServiceRecords().addAll(serviceRecordFactory.createDigitalpostServiceRecords(identifier, auth, clientOrgnr));
        }
        if (processCategory == ProcessCategory.ARKIVMELDING) {
            Optional<ServiceRecord> osr = serviceRecordFactory.createArkivmeldingServiceRecord(identifier, processIdentifier, securityLevel);
            if (osr.isPresent()) {
                serviceRecord = osr.get();
            }
            if (serviceRecord == null) {
                return notFoundResponse(String.format("Arkivmelding process '%s' not found for receiver '%s'.", processIdentifier, identifier));
            }
        }
        if (processCategory == ProcessCategory.EINNSYN) {
            Optional<ServiceRecord> dpeServiceRecord = serviceRecordFactory.createEinnsynServiceRecord(identifier, processIdentifier);
            if (!dpeServiceRecord.isPresent()) {
                return notFoundResponse(String.format("eInnsyn process '%s' not found for receiver '%s'.", processIdentifier, identifier));
            }
            serviceRecord = dpeServiceRecord.get();
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
    public ResponseEntity entity(
            @PathVariable("identifier") String identifier,
            @RequestParam(name = "securityLevel", required = false) Integer securityLevel,
            @RequestParam(name = "conversationId", required = false) String conversationId,
            Authentication auth,
            HttpServletRequest request)
            throws SecurityLevelNotFoundException, KRRClientException, CertificateNotFoundException, DsfLookupException, BrregNotFoundException, SvarUtClientException {
        MDC.put("identifier", identifier);
        requestScope.setConversationId(conversationId);
        requestScope.setIdentifier(identifier);
        String clientOrgnr = authenticationService.getOrganizationNumber(auth, request);
        requestScope.setClientId(clientOrgnr);
        Entity entity = new Entity();
        Optional<EntityInfo> entityInfo = entityService.getEntityInfo(identifier);
        if (!entityInfo.isPresent()) {
            return notFoundResponse(String.format("Entity with identifier '%s' not found.", identifier));
        }
        entity.setInfoRecord(entityInfo.get());
        if (shouldCreateServiceRecordForCitizen().test(entityInfo.get())) {
            if (clientOrgnr == null) {
                return errorResponse(HttpStatus.UNAUTHORIZED, "No authentication provided.");
            }
            entity.getServiceRecords().addAll(serviceRecordFactory.createDigitalpostServiceRecords(identifier, auth, clientOrgnr));
        } else {
            entity.getServiceRecords().addAll(serviceRecordFactory.createArkivmeldingServiceRecords(identifier, auth, securityLevel));
            entity.getServiceRecords().addAll(serviceRecordFactory.createEinnsynServiceRecords(identifier));
        }
        return new ResponseEntity<>(entity, HttpStatus.OK);
    }

    private ResponseEntity errorResponse(HttpStatus status, String message) {
        return ResponseEntity.status(status)
                .body(ErrorResponse.builder().errorDescription(message).build());
    }

    private ResponseEntity notFoundResponse(String logMessage) {
        log.error(markerFrom(requestScope), logMessage);
        return ResponseEntity.notFound().build();
    }

    @GetMapping(value = "/identifier/{identifier}", produces = "application/jose")
    @ResponseBody
    public ResponseEntity signed(
            @PathVariable("identifier") String identifier,
            @RequestParam(name = "securityLevel", required = false) Integer securityLevel,
            @RequestParam(name = "conversationId", required = false) String conversationId,
            Authentication auth,
            HttpServletRequest request)
            throws EntitySignerException, SecurityLevelNotFoundException, KRRClientException,
            CertificateNotFoundException, DsfLookupException, BrregNotFoundException, SvarUtClientException {
        ResponseEntity entity = entity(identifier, securityLevel, conversationId, auth, request);
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

    @GetMapping(value = "/identifier/{identifier}/process/{processIdentifier}", produces = "application/jose")
    @ResponseBody
    public ResponseEntity signed(@PathVariable("identifier") String identifier,
                                 @PathVariable("processIdentifier") String processIdentifier,
                                 @RequestParam(name = "securityLevel", required = false) Integer securityLevel,
                                 @RequestParam(name = "conversationId", required = false) String conversationId,
                                 Authentication auth,
                                 HttpServletRequest request)
            throws SecurityLevelNotFoundException, KRRClientException, CertificateNotFoundException,
            DsfLookupException, BrregNotFoundException, SvarUtClientException, EntitySignerException {
        ResponseEntity entity = entity(identifier, processIdentifier, securityLevel, conversationId, auth, request);
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

    @GetMapping(value = "/info/{identifier}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity info(@PathVariable("identifier") String identifier) {
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
    public ResponseEntity signed(@PathVariable("identifier") String identifier) throws EntitySignerException {
        ResponseEntity entity = info(identifier);
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

    @ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "Could not find endpoint url for service of requested organization")
    @ExceptionHandler(EndpointUrlNotFound.class)
    public void endpointNotFound(HttpServletRequest req, Exception e) {
        log.warn(markerFrom(requestScope), "Endpoint not found for {}", req.getRequestURL(), e);
    }

    @ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "Could not find entity for the requested identifier")
    @ExceptionHandler(EntityNotFoundException.class)
    public void entityNotFound(HttpServletRequest req, Exception e) {
        log.warn(markerFrom(requestScope), "Entity not found for {}", req.getRequestURL(), e);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity accessDenied(HttpServletRequest req, Exception e) {
        log.warn(markerFrom(requestScope), "Access denied on resource {}", req.getRequestURL(), e);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized scope");
    }

    @ExceptionHandler(SecurityLevelNotFoundException.class)
    public ResponseEntity securityLevelNotFound(HttpServletRequest request, Exception e) {
        log.warn(markerFrom(requestScope), "Security level not found for {}", request.getRequestURL(), e);
        return ResponseEntity.badRequest().body(ErrorResponse.builder().errorDescription(e.getMessage()).build());
    }

    @ExceptionHandler(KRRClientException.class)
    public ResponseEntity krrClientException(HttpServletRequest request, Exception e) {
        log.error(markerFrom(requestScope), "Exception occurred on {}", request.getRequestURL(), e);
        return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
    }

    @ExceptionHandler(SvarUtClientException.class)
    public ResponseEntity handleSvarUtClientException(HttpServletRequest request, Exception e) {
        log.error(markerFrom(requestScope), "Exception occurred on {}", request.getRequestURL(), e);
        return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
    }

    @ExceptionHandler(CertificateNotFoundException.class)
    public ResponseEntity certificateNotFound(HttpServletRequest request, Exception e) {
        log.warn(markerFrom(requestScope), "Certificate not found for {}", request.getRequestURL(), e);
        return errorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(DsfLookupException.class)
    public ResponseEntity dsfLookupException(HttpServletRequest request, Exception e) {
        log.warn(markerFrom(requestScope), "DSF lookup failed for {}", request.getRequestURL(), e);
        return errorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(BrregNotFoundException.class)
    public ResponseEntity brregNotFoundException(HttpServletRequest request, Exception e) {
        log.warn(markerFrom(requestScope), "BRREG lookup failed for {}", request.getRequestURL(), e);
        return errorResponse(HttpStatus.NOT_FOUND, e.getMessage());
    }
}

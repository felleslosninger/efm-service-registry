package no.difi.meldingsutveksling.serviceregistry.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.difi.meldingsutveksling.Notification;
import no.difi.meldingsutveksling.serviceregistry.EntityNotFoundException;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryException;
import no.difi.meldingsutveksling.serviceregistry.exceptions.EndpointUrlNotFound;
import no.difi.meldingsutveksling.serviceregistry.exceptions.SecurityLevelNotFoundException;
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
import no.difi.meldingsutveksling.serviceregistry.servicerecord.ServiceRecord;
import no.difi.meldingsutveksling.serviceregistry.servicerecord.ServiceRecordFactory;
import org.jboss.logging.MDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

    /**
     * @param serviceRecordFactory for creation of the identifiers respective service record
     * @param entityService        needed to lookup and retrieve organization or citizen information using an identifier number
     * @param processService
     * @param authenticationService
     */
    @Autowired
    public ServiceRecordController(ServiceRecordFactory serviceRecordFactory,
                                   EntityService entityService,
                                   PayloadSigner payloadSigner, ProcessService processService, AuthenticationService authenticationService) {
        this.entityService = entityService;
        this.serviceRecordFactory = serviceRecordFactory;
        this.payloadSigner = payloadSigner;
        this.processService = processService;
        this.authenticationService = authenticationService;
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
     * @param obligation        determines service record based on the recipient being notifiable
     * @param forcePrint
     * @param auth              provides the authentication object.
     * @param request           is the servlet request.
     * @return JSON object with information needed to send a message.
     */
    @GetMapping(value = "/identifier/{identifier}/process/{processIdentifier}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity entity(@PathVariable("identifier") String identifier,
                                 @PathVariable("processIdentifier") String processIdentifier,
                                 @RequestParam(name = "notification", defaultValue = "NOT_OBLIGATED") Notification obligation,
                                 @RequestParam(name = "forcePrint", defaultValue = "false") boolean forcePrint,
                                 @RequestParam(name = "securityLevel", required = false) Integer securityLevel,
                                 Authentication auth,
                                 HttpServletRequest request) throws SecurityLevelNotFoundException {
        MDC.put("entity", identifier);
        Optional<EntityInfo> entityInfo = entityService.getEntityInfo(identifier);
        if (!entityInfo.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        Optional<Process> optionalProcess = processService.findByIdentifier(processIdentifier);
        if (!optionalProcess.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        ServiceRecord serviceRecord = null;
        Process process = optionalProcess.get();
        ProcessCategory processCategory = process.getCategory();
        if (processCategory.equals(ProcessCategory.DIGITALPOST) && shouldCreateServiceRecordForCitizen().test(entityInfo.get())) {
            String clientOrgnr = authenticationService.getAuthorizedClientIdentifier(auth, request);
            if (clientOrgnr == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ErrorResponse.builder().errorDescription("No authentication provided.").build());
            }
            try {
                Entity entity = new Entity();
                entity.getServiceRecords().addAll(serviceRecordFactory.createDigitalpostServiceRecords(identifier, auth, clientOrgnr, obligation, forcePrint));
                entity.setInfoRecord(entityInfo.get());
                return new ResponseEntity<>(entity, HttpStatus.OK);
            } catch (KRRClientException e) {
                log.error("Error looking up identifier in KRR", e);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
            }
        }

        Entity entity = new Entity();
        Optional<ServiceRecord> osr;
        if (processCategory == ProcessCategory.ARKIVMELDING) {
            osr = serviceRecordFactory.createArkivmeldingServiceRecord(identifier, processIdentifier, securityLevel);
            if (osr.isPresent()){
                serviceRecord = osr.get();
            }
            if (serviceRecord == null) {
                return ResponseEntity.badRequest().body(String.format("Process %s not found for receiver %s", processIdentifier, identifier));
            }
        }

        if (processCategory == ProcessCategory.EINNSYN) {
            Optional<ServiceRecord> dpeServiceRecord = serviceRecordFactory.createEinnsynServiceRecord(identifier, processIdentifier);
            if (!dpeServiceRecord.isPresent()) {
                log.error(String.format("Process %s not found for receiver %s", processIdentifier, identifier));
                return ResponseEntity.notFound().build();
            }
            serviceRecord = dpeServiceRecord.get();
        }

        entity.setInfoRecord(entityInfo.get());
        entity.getServiceRecords().add(serviceRecord);
        return new ResponseEntity<>(entity, HttpStatus.OK);
    }

    /**
     * Used to retrieve information needed to send a message to an entity (organization or a person) having the provided
     * identifier
     *
     * @param identifier of the organization/person to receive a message
     * @param obligation determines service record based on the recipient being notifiable
     * @return JSON object with information needed to send a message
     */
    @GetMapping(value = "/identifier/{identifier}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @SuppressWarnings("squid:S2583")
    public ResponseEntity entity(
            @PathVariable("identifier") String identifier,
            @RequestParam(name = "notification", defaultValue = "NOT_OBLIGATED") Notification obligation,
            @RequestParam(name = "forcePrint", defaultValue = "false") boolean forcePrint,
            @RequestParam(name = "securityLevel", required = false) Integer securityLevel,
            Authentication auth,
            HttpServletRequest request) throws SecurityLevelNotFoundException, KRRClientException {
        MDC.put("identifier", identifier);
        Entity entity = new Entity();
        Optional<EntityInfo> entityInfo = entityService.getEntityInfo(identifier);
        if (!entityInfo.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        entity.setInfoRecord(entityInfo.get());
        if (shouldCreateServiceRecordForCitizen().test(entityInfo.get())) {
            String clientOrgnr = authenticationService.getAuthorizedClientIdentifier(auth, request);
            if (clientOrgnr == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ErrorResponse.builder().errorDescription("No authentication provided.").build());
            }
            entity.getServiceRecords().addAll(serviceRecordFactory.createDigitalpostServiceRecords(identifier, auth, clientOrgnr, obligation, forcePrint));
        }
        entity.getServiceRecords().addAll(serviceRecordFactory.createArkivmeldingServiceRecords(identifier, securityLevel));
        entity.getServiceRecords().addAll(serviceRecordFactory.createEinnsynServiceRecords(identifier));
        return new ResponseEntity<>(entity, HttpStatus.OK);
    }


    @RequestMapping(value = "/identifier/{identifier}", method = RequestMethod.GET, produces = "application/jose")
    @ResponseBody
    public ResponseEntity signed(
            @PathVariable("identifier") String identifier,
            @RequestParam(name = "notification", defaultValue = "NOT_OBLIGATED") Notification obligation,
            @RequestParam(name = "forcePrint", defaultValue = "false") boolean forcePrint,
            @RequestParam(name = "securityLevel", required = false) Integer securityLevel,
            Authentication auth,
            HttpServletRequest request) throws EntitySignerException, SecurityLevelNotFoundException, KRRClientException {

        ResponseEntity entity = entity(identifier, obligation, forcePrint, securityLevel, auth, request);
        if (entity.getStatusCode() != HttpStatus.OK) {
            return entity;
        }

        String json;
        try {
            json = new ObjectMapper().writeValueAsString(entity.getBody());
        } catch (JsonProcessingException e) {
            log.error("Failed to convert entity to json", e);
            throw new ServiceRegistryException(e);
        }

        return ResponseEntity.ok(payloadSigner.sign(json));
    }

    @ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "Could not find endpoint url for service of requested organization")
    @ExceptionHandler(EndpointUrlNotFound.class)
    public void endpointNotFound(HttpServletRequest req, Exception e) {
        log.warn(String.format("Endpoint not found for %s", req.getRequestURL()), e);
    }

    @ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "Could not find entity for the requested identifier")
    @ExceptionHandler(EntityNotFoundException.class)
    public void entityNotFound(HttpServletRequest req, Exception e) {
        log.warn(String.format("Entity not found for %s", req.getRequestURL()), e);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity accessDenied(HttpServletRequest req, Exception e) {
        log.warn("Access denied on resource {}", req.getRequestURL(), e);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized scope");
    }

    @ExceptionHandler(SecurityLevelNotFoundException.class)
    public ResponseEntity securityLevelNotFound(HttpServletRequest request, Exception e) {
        log.warn(String.format("Security level not found for %s", request.getRequestURL()));
        return ResponseEntity.badRequest().body(ErrorResponse.builder().errorDescription(e.getMessage()).build());
    }

    @ExceptionHandler(KRRClientException.class)
    public ResponseEntity krrClientException(HttpServletRequest request, Exception e) {
        log.error("Exception occurred on {}", request.getRequestURL(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.builder().errorDescription(e.getMessage()).build());
    }

}

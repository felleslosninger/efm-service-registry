package no.difi.meldingsutveksling.serviceregistry.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.difi.meldingsutveksling.Notification;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.meldingsutveksling.serviceregistry.EntityNotFoundException;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryException;
import no.difi.meldingsutveksling.serviceregistry.exceptions.EndpointUrlNotFound;
import no.difi.meldingsutveksling.serviceregistry.krr.KRRClientException;
import no.difi.meldingsutveksling.serviceregistry.model.Entity;
import no.difi.meldingsutveksling.serviceregistry.model.EntityInfo;
import no.difi.meldingsutveksling.serviceregistry.security.EntitySignerException;
import no.difi.meldingsutveksling.serviceregistry.security.PayloadSigner;
import no.difi.meldingsutveksling.serviceregistry.service.EntityService;
import no.difi.meldingsutveksling.serviceregistry.service.ks.FiksAdresseClient;
import no.difi.meldingsutveksling.serviceregistry.service.ks.FiksAdressing;
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

import static no.difi.meldingsutveksling.serviceregistry.businesslogic.ServiceRecordPredicates.shouldCreateServiceRecordForCititzen;
import static no.difi.meldingsutveksling.serviceregistry.logging.SRMarkerFactory.markerFrom;

@ExposesResourceFor(Entity.class)
@RestController
public class ServiceRecordController {

    private static final Logger log = LoggerFactory.getLogger(ServiceRecordController.class);
    private final ServiceRecordFactory serviceRecordFactory;
    private EntityService entityService;
    private PayloadSigner payloadSigner;
    private FiksAdresseClient fiksAdresseClient;

    /**
     * @param serviceRecordFactory for creation of the identifiers respective service record
     * @param entityService needed to lookup and retrieve organization or citizen information using an identifier number
     */
    @Autowired
    public ServiceRecordController(ServiceRecordFactory serviceRecordFactory,
                                   EntityService entityService,
                                   PayloadSigner payloadSigner,
                                   FiksAdresseClient fiksAdresseClient) {
        this.entityService = entityService;
        this.serviceRecordFactory = serviceRecordFactory;
        this.payloadSigner = payloadSigner;
        this.fiksAdresseClient = fiksAdresseClient;
    }

    @InitBinder
    protected void initBinders(WebDataBinder binder) {
        binder.registerCustomEditor(Notification.class, new NotificationEditor());
    }

    /**
     * Used to retrieve information needed to send a message to an entity (organization or a person) having the provided
     * identifier
     *
     * @param identifier of the organization/person to receive a message
     * @param obligation determines service record based on the recipient being notifiable
     * @return JSON object with information needed to send a message
     */
    @RequestMapping(value = "/identifier/{identifier}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity entity(
            @PathVariable("identifier") String identifier,
            @RequestParam(name="notification", defaultValue="NOT_OBLIGATED") Notification obligation,
            Authentication auth,
            HttpServletRequest request) {

        MDC.put("identifier", identifier);
        Entity entity = new Entity();
        Optional<EntityInfo> entityInfo = entityService.getEntityInfo(identifier);
        if (!entityInfo.isPresent()) {
            log.warn("Could not find entity for the requeste identifier={}", identifier);
            return ResponseEntity.notFound().build();
        }

        String clientOrgnr = auth == null ? null : (String) auth.getPrincipal();
        if (clientOrgnr != null) {
            Audit.info("Authorized lookup request", markerFrom(request.getRemoteAddr(), clientOrgnr));
        } else {
            Audit.info("Unauthorized lookup request", markerFrom(request.getRemoteAddr()));
        }


        Optional<ServiceRecord> serviceRecord = Optional.empty();

        if (shouldCreateServiceRecordForCititzen().test(entityInfo.get())) {
            if (auth == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("No authentication provided.");
            }
            try {
                serviceRecord = serviceRecordFactory.createServiceRecordForCititzen(identifier, auth, clientOrgnr, obligation);
            } catch (KRRClientException e) {
                log.error("Error looking up identifier in KRR", e);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
            }
        }

        if (!serviceRecord.isPresent()) {
            serviceRecord = serviceRecordFactory.createEduServiceRecord(identifier);
        }

        if(!serviceRecord.isPresent()) {
            final FiksAdressing fiksAdressing = fiksAdresseClient.getFiksAdressing(entityInfo.get().getIdentifier());
            serviceRecord = serviceRecordFactory.createFiksServiceRecord(fiksAdressing);
        }

        if (!serviceRecord.isPresent()) {
            serviceRecord = serviceRecordFactory.createDpeServiceRecord(identifier);
        }

        if (!serviceRecord.isPresent()) {
            serviceRecord = serviceRecordFactory.createPostVirksomhetServiceRecord(identifier);
        }

        serviceRecord.ifPresent(entity::setServiceRecord);
        entity.setInfoRecord(entityInfo.get());

        return new ResponseEntity<>(entity, HttpStatus.OK);
    }

    @RequestMapping(value = "/identifier/{identifier}", method = RequestMethod.GET, produces = "application/jose")
    @ResponseBody
    public ResponseEntity signed(
        @PathVariable("identifier") String identifier,
        @RequestParam(name="notification", defaultValue="NOT_OBLIGATED") Notification obligation,
        Authentication auth,
        HttpServletRequest request) throws EntitySignerException {

        ResponseEntity entity = entity(identifier, obligation, auth, request);
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

}

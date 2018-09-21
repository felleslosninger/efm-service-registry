package no.difi.meldingsutveksling.serviceregistry.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.difi.meldingsutveksling.Notification;
import no.difi.meldingsutveksling.serviceregistry.EntityNotFoundException;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryException;
import no.difi.meldingsutveksling.serviceregistry.exceptions.EndpointUrlNotFound;
import no.difi.meldingsutveksling.serviceregistry.krr.KRRClientException;
import no.difi.meldingsutveksling.serviceregistry.model.Entity;
import no.difi.meldingsutveksling.serviceregistry.model.EntityInfo;
import no.difi.meldingsutveksling.serviceregistry.model.ServiceIdentifier;
import no.difi.meldingsutveksling.serviceregistry.security.EntitySignerException;
import no.difi.meldingsutveksling.serviceregistry.security.PayloadSigner;
import no.difi.meldingsutveksling.serviceregistry.service.EntityService;
import no.difi.meldingsutveksling.serviceregistry.servicerecord.ErrorServiceRecord;
import no.difi.meldingsutveksling.serviceregistry.servicerecord.FiksWrapper;
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

    /**
     * @param serviceRecordFactory for creation of the identifiers respective service record
     * @param entityService        needed to lookup and retrieve organization or citizen information using an identifier number
     */
    @Autowired
    public ServiceRecordController(ServiceRecordFactory serviceRecordFactory,
                                   EntityService entityService,
                                   PayloadSigner payloadSigner) {
        this.entityService = entityService;
        this.serviceRecordFactory = serviceRecordFactory;
        this.payloadSigner = payloadSigner;
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
    @SuppressWarnings("squid:S2583")
    public ResponseEntity entity(
            @PathVariable("identifier") String identifier,
            @RequestParam(name = "notification", defaultValue = "NOT_OBLIGATED") Notification obligation,
            @RequestParam(name = "forcePrint", defaultValue = "false") boolean forcePrint,
            Authentication auth,
            HttpServletRequest request) {

        MDC.put("identifier", identifier);
        Entity entity = new Entity();
        Optional<EntityInfo> entityInfo = entityService.getEntityInfo(identifier);
        if (!entityInfo.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        String clientOrgnr = auth == null ? null : (String) auth.getPrincipal();
        if (clientOrgnr != null) {
            log.debug(String.format("Authorized lookup request by %s", clientOrgnr),
                    markerFrom(request.getRemoteAddr(), request.getRemoteHost(), clientOrgnr));
        } else {
            log.debug(String.format("Unauthorized lookup request from %s", request.getRemoteAddr()),
                    markerFrom(request.getRemoteAddr()));
        }


        Optional<ServiceRecord> serviceRecord = Optional.empty();

        if (shouldCreateServiceRecordForCititzen().test(entityInfo.get())) {
            if (auth == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("No authentication provided.");
            }
            try {
                serviceRecord = serviceRecordFactory.createServiceRecordForCititzen(identifier, auth, clientOrgnr, obligation, forcePrint);
            } catch (KRRClientException e) {
                log.error("Error looking up identifier in KRR", e);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
            }
        }

        if (!serviceRecord.isPresent()) {
            serviceRecord = serviceRecordResponseHandler(serviceRecordFactory.createEduServiceRecord(identifier), entity);
        }

        if (!serviceRecord.isPresent()) {
            Optional<FiksWrapper> fiksWrapper = serviceRecordFactory.createFiksServiceRecord(identifier);
            if (fiksWrapper.isPresent()) {
                serviceRecord = serviceRecordResponseHandler(Optional.of(fiksWrapper.get().getServiceRecord()), entity);
                if (serviceRecord.isPresent()) {
                    entity.getSecuritylevels().put(ServiceIdentifier.DPF, fiksWrapper.get().getSecuritylevel());
                }
            }
        }

        if (!serviceRecord.isPresent()) {
            serviceRecord = serviceRecordResponseHandler(serviceRecordFactory.createDpeInnsynServiceRecord(identifier), entity);
        }

        if (!serviceRecord.isPresent()) {
            serviceRecord = serviceRecordResponseHandler(serviceRecordFactory.createPostVirksomhetServiceRecord(identifier), entity);
        }

        serviceRecord.ifPresent(entity::setServiceRecord);
        entity.setInfoRecord(entityInfo.get());
        // TODO: temporary solution for multiple servicerecords
        addServiceRecords(entityInfo.get(), entity, auth, clientOrgnr, obligation, forcePrint);

        return new ResponseEntity<>(entity, HttpStatus.OK);
    }

    @SuppressWarnings("squid:S2583")
    private void addServiceRecords(EntityInfo entityInfo, Entity entity, Authentication auth, String clientOrgnr,
                                   Notification obligation, boolean forcePrint) {

        String orgnr = entityInfo.getIdentifier();
        if (shouldCreateServiceRecordForCititzen().test(entityInfo)) {
            Optional<ServiceRecord> serviceRecord = Optional.empty();
            try {
                serviceRecord = serviceRecordFactory.createServiceRecordForCititzen(orgnr, auth, clientOrgnr, obligation, forcePrint);
            } catch (KRRClientException e) {
                log.error("Error looking up identifier in KRR", e);
            }
            serviceRecord.ifPresent(r -> entity.getServiceRecords().add(r));
        }

        Optional<ServiceRecord> eduServiceRecord = serviceRecordResponseHandler(serviceRecordFactory.createEduServiceRecord(orgnr), entity);
        eduServiceRecord.ifPresent(r -> entity.getServiceRecords().add(r));

        Optional<FiksWrapper> fiksWrapper = serviceRecordFactory.createFiksServiceRecord(orgnr);
        if (fiksWrapper.isPresent()) {
            Optional<ServiceRecord> fiksServiceRecord = serviceRecordResponseHandler(Optional.of(fiksWrapper.get().getServiceRecord()), entity);
            if (fiksServiceRecord.isPresent()) {
                entity.getServiceRecords().add(fiksServiceRecord.get());
                entity.getSecuritylevels().put(ServiceIdentifier.DPF, fiksWrapper.get().getSecuritylevel());
            }
        }

        Optional<ServiceRecord> dpeInnsynServiceRecord = serviceRecordResponseHandler(serviceRecordFactory.createDpeInnsynServiceRecord(orgnr), entity);
        dpeInnsynServiceRecord.ifPresent(r -> entity.getServiceRecords().add(r));

        Optional<ServiceRecord> dpeDataServiceRecord = serviceRecordResponseHandler(serviceRecordFactory.createDpeDataServiceRecord(orgnr), entity);
        dpeDataServiceRecord.ifPresent(r -> entity.getServiceRecords().add(r));

        if (dpeInnsynServiceRecord.isPresent() || dpeDataServiceRecord.isPresent()) {
            Optional<ServiceRecord> dpeReceiptServiceRecord = serviceRecordResponseHandler(serviceRecordFactory.createDpeReceiptServiceRecord(orgnr), entity);
            dpeReceiptServiceRecord.ifPresent(r -> entity.getServiceRecords().add(r));
        }

        Optional<ServiceRecord> dpvServiceRecord = serviceRecordFactory.createPostVirksomhetServiceRecord(orgnr);
        dpvServiceRecord.ifPresent(r -> entity.getServiceRecords().add(r));

    }

    @RequestMapping(value = "/identifier/{identifier}", method = RequestMethod.GET, produces = "application/jose")
    @ResponseBody
    public ResponseEntity signed(
            @PathVariable("identifier") String identifier,
            @RequestParam(name = "notification", defaultValue = "NOT_OBLIGATED") Notification obligation,
            @RequestParam(name = "forcePrint", defaultValue = "false") boolean forcePrint,
            Authentication auth,
            HttpServletRequest request) throws EntitySignerException {

        ResponseEntity entity = entity(identifier, obligation, forcePrint, auth, request);
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

    /**
     * Checks if the returned service record is of type ErrorServiceRecord.
     * If so, add the service identifier to the list of failed services and
     * return empty {@code Optional}. Else, return the service record.
     *
     * @param serviceRecord to check
     * @param entity to add failed serviceIdentifiers to
     * @return {@code Optional} if present, empty otherwise
     */
    private Optional<ServiceRecord> serviceRecordResponseHandler(Optional<ServiceRecord> serviceRecord, Entity entity) {
        if (serviceRecord.filter(r -> r instanceof ErrorServiceRecord).isPresent()) {
            if (!entity.getFailedServiceIdentifiers().contains(serviceRecord.get().getServiceIdentifier())) {
                entity.getFailedServiceIdentifiers().add(serviceRecord.get().getServiceIdentifier());
            }
            return Optional.empty();
        }
        return serviceRecord;
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

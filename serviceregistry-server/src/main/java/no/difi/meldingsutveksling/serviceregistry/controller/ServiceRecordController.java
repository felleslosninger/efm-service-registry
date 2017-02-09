package no.difi.meldingsutveksling.serviceregistry.controller;

import no.difi.meldingsutveksling.Notification;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.meldingsutveksling.serviceregistry.CertificateNotFoundException;
import no.difi.meldingsutveksling.serviceregistry.EntityNotFoundException;
import no.difi.meldingsutveksling.serviceregistry.exceptions.EndpointUrlNotFound;
import no.difi.meldingsutveksling.serviceregistry.model.Entity;
import no.difi.meldingsutveksling.serviceregistry.model.EntityInfo;
import no.difi.meldingsutveksling.serviceregistry.security.EntitySigner;
import no.difi.meldingsutveksling.serviceregistry.security.EntitySignerException;
import no.difi.meldingsutveksling.serviceregistry.service.EntityService;
import no.difi.meldingsutveksling.serviceregistry.service.ks.FiksAdresseClient;
import no.difi.meldingsutveksling.serviceregistry.service.ks.FiksAdressing;
import no.difi.meldingsutveksling.serviceregistry.servicerecord.ServiceRecordFactory;
import org.jboss.logging.MDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetails;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

import static no.difi.meldingsutveksling.serviceregistry.businesslogic.ServiceRecordPredicates.*;
import static no.difi.meldingsutveksling.serviceregistry.logging.SRMarkerFactory.markerFrom;

@ExposesResourceFor(Entity.class)
@RestController
public class ServiceRecordController {

    private static final Logger logger = LoggerFactory.getLogger(ServiceRecordController.class);
    private final ServiceRecordFactory serviceRecordFactory;
    private EntityService entityService;
    private EntitySigner entitySigner;
    private FiksAdresseClient fiksAdresseClient;

    /**
     * @param serviceRecordFactory for creation of the identifiers respective service record
     * @param entityService needed to lookup and retrieve organization or citizen information using an identifier number
     */
    @Autowired
    public ServiceRecordController(ServiceRecordFactory serviceRecordFactory,
                                   EntityService entityService,
                                   EntitySigner entitySigner,
                                   FiksAdresseClient fiksAdresseClient) {
        this.entityService = entityService;
        this.serviceRecordFactory = serviceRecordFactory;
        this.entitySigner = entitySigner;
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
    @RequestMapping(value = "/identifier/{identifier}", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public ResponseEntity entity(
            @PathVariable("identifier") String identifier,
            @RequestParam(name="notification", defaultValue="NOT_OBLIGATED") Notification obligation,
            Authentication auth,
            HttpServletRequest request) {

        MDC.put("identifier", identifier);
        Entity entity = new Entity();
        EntityInfo entityInfo = entityService.getEntityInfo(identifier);
        if (entityInfo == null) {
            throw new EntityNotFoundException("Could not find entity for identifier: " + identifier);
        }

        String clientOrgnr = auth == null ? null : (String) auth.getPrincipal();
        if (clientOrgnr != null) {
            String tokenValue = ((OAuth2AuthenticationDetails) auth.getDetails()).getTokenValue();
            Audit.info("Authorized lookup request", markerFrom(request.getRemoteAddr(), clientOrgnr, tokenValue));
        } else {
            Audit.info("Unauthorized lookup request", markerFrom(request.getRemoteAddr()));
        }

        final FiksAdressing fiksAdressing = fiksAdresseClient.getFiksAdressing(entityInfo.getIdentifier());

        if (shouldCreateServiceRecordForCititzen().test(entityInfo)) {
            if (auth == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("No authentication provided.");
            }
            entity.setServiceRecord(serviceRecordFactory.createServiceRecordForCititzen(identifier, clientOrgnr,
                    obligation));
        } else if(fiksAdressing.shouldUseFIKS()) {
            entity.setServiceRecord(serviceRecordFactory.createFiksServiceRecord(fiksAdressing));
            entity.setInfo(entityInfo);
            EntityResource organizationRes = new EntityResource(entity);
            return new ResponseEntity<>(organizationRes, HttpStatus.OK);
        }

        if (usesFormidlingstjenesten().test(entityInfo)) {
            entity.setServiceRecord(serviceRecordFactory.createEduServiceRecord(identifier));
        }
        if (usesPostTilVirksomhet().test(entityInfo) || entity.getServiceRecord() == null) {
            entity.setServiceRecord(serviceRecordFactory.createPostVirksomhetServiceRecord(identifier));
        }

        entity.setInfoRecord(entityInfo);
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

        return ResponseEntity.ok(entitySigner.sign((Entity)entity.getBody()));
    }

    @ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "Could not find certificate for requested organization")
    @ExceptionHandler(CertificateNotFoundException.class)
    public void certificateNotFound(HttpServletRequest req, Exception e) {
        logger.warn("Certificate not found for: " + req.getRequestURL().toString(), e);
    }

    @ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "Could not find endpoint url for service of requested organization")
    @ExceptionHandler(EndpointUrlNotFound.class)
    public void endpointNotFound(HttpServletRequest req, Exception e) {
        logger.warn(String.format("Endpoint not found for %s", req.getRequestURL()), e);
    }

    @ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "Could not find entity for the requested identifier")
    @ExceptionHandler(EntityNotFoundException.class)
    public void entityNotFound(HttpServletRequest req, Exception e) {
        logger.warn(String.format("Entity not found for %s", req.getRequestURL()), e);
    }

}

package no.difi.meldingsutveksling.serviceregistry.controller;

import no.difi.meldingsutveksling.serviceregistry.CertificateNotFoundException;
import no.difi.meldingsutveksling.serviceregistry.EntityNotFoundException;
import no.difi.meldingsutveksling.serviceregistry.exceptions.EndpointUrlNotFound;
import no.difi.meldingsutveksling.serviceregistry.model.Entity;
import no.difi.meldingsutveksling.serviceregistry.model.EntityInfo;
import no.difi.meldingsutveksling.serviceregistry.service.EntityService;
import no.difi.meldingsutveksling.serviceregistry.servicerecord.ServiceRecordFactory;
import org.jboss.logging.MDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

import static no.difi.meldingsutveksling.serviceregistry.businesslogic.ServiceRecordPredicates.usesFormidlingstjenesten;
import static no.difi.meldingsutveksling.serviceregistry.businesslogic.ServiceRecordPredicates.usesPostTilVirksomhet;
import static no.difi.meldingsutveksling.serviceregistry.businesslogic.ServiceRecordPredicates.usesSikkerDigitalPost;

@RequestMapping("/identifier")
@ExposesResourceFor(EntityResource.class)
@RestController
public class ServiceRecordController {

    private final ServiceRecordFactory serviceRecordFactory;
    private EntityService entityService;
    private static final Logger logger = LoggerFactory.getLogger(ServiceRecordController.class);

    /**
     * @param serviceRecordFactory for creation of the identifiers respective service record
     * @param entityService needed to lookup and retrieve organization or citizen information using an identifier number
     */
    @Autowired
    public ServiceRecordController(ServiceRecordFactory serviceRecordFactory,
            EntityService entityService) {
        this.entityService = entityService;
        this.serviceRecordFactory = serviceRecordFactory;
    }

    /**
     * Used to retrieve information needed to send a message to an entity (organization or a person) having the provided
     * identifier
     *
     * @param identifier of the organization/person to receive a message
     * @return JSON object with information needed to send a message
     */
    @RequestMapping("/{identifier}")
    @ResponseBody
    public ResponseEntity entity(@PathVariable("identifier") String identifier, Authentication auth) {
        MDC.put("identifier", identifier);
        Entity entity = new Entity();
        EntityInfo entityInfo = entityService.getEntityInfo(identifier);
        String clientOrgnr = auth == null ? null : (String) auth.getPrincipal();
        if (entityInfo == null) {
            throw new EntityNotFoundException("Could not find entity for identifier: " + identifier);
        }

        if (usesSikkerDigitalPost().test(entityInfo)) {
            if (auth == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("No authentication provided.");
            }
            entity.setServiceRecord(serviceRecordFactory.createSikkerDigitalPostRecord(identifier, clientOrgnr));
        }
        if (usesFormidlingstjenesten().test(entityInfo)) {
            entity.setServiceRecord(serviceRecordFactory.createEduServiceRecord(identifier));
        }
        if (usesPostTilVirksomhet().test(entityInfo)) {
            entity.setServiceRecord(serviceRecordFactory.createPostVirksomhetServiceRecord(identifier));
        }
        entity.setInfo(entityInfo);
        EntityResource organizationRes = new EntityResource(entity);
        return new ResponseEntity<>(organizationRes, HttpStatus.OK);
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

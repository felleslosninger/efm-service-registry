package no.difi.meldingsutveksling.serviceregistry.controller;

import javax.servlet.http.HttpServletRequest;
import no.difi.meldingsutveksling.serviceregistry.CertificateNotFoundException;
import no.difi.meldingsutveksling.serviceregistry.EntityNotFoundException;
import static no.difi.meldingsutveksling.serviceregistry.businesslogic.ServiceRecordPredicates.*;
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
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity entity(@PathVariable("identifier") String identifier, OAuth2Authentication auth) {
        MDC.put("identifier", identifier);
        Entity entity = new Entity();
        EntityInfo entityInfo = entityService.getEntityInfo(identifier);
        // TODO: send clientOrgnr videre til KRR
        String clientOrgnr = (String) auth.getPrincipal();
        if (entityInfo == null) {
            throw new EntityNotFoundException("Could not find entity for identifier: " + identifier);
        }

        if (usesSikkerDigitalPost().test(entityInfo)) {
            entity.setServiceRecord(serviceRecordFactory.createSikkerDigitalPostRecord(identifier));
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

package no.difi.meldingsutveksling.serviceregistry.controller;


import no.difi.meldingsutveksling.serviceregistry.CertificateNotFoundException;
import no.difi.meldingsutveksling.serviceregistry.EntityNotFoundException;
import no.difi.meldingsutveksling.serviceregistry.exceptions.EndpointUrlNotFound;
import no.difi.meldingsutveksling.serviceregistry.model.Entity;
import no.difi.meldingsutveksling.serviceregistry.model.EntityInfo;
import no.difi.meldingsutveksling.serviceregistry.model.ServiceIdentifier;
import no.difi.meldingsutveksling.serviceregistry.service.EntityService;
import no.difi.meldingsutveksling.serviceregistry.service.elma.ELMALookupService;
import no.difi.meldingsutveksling.serviceregistry.service.krr.KrrService;
import no.difi.meldingsutveksling.serviceregistry.service.ks.KSLookup;
import no.difi.meldingsutveksling.serviceregistry.service.persistence.PrimaryServiceStore;
import no.difi.meldingsutveksling.serviceregistry.service.virksert.VirkSertService;
import no.difi.meldingsutveksling.serviceregistry.servicerecord.ServiceRecordFactory;
import org.jboss.logging.MDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

import static no.difi.meldingsutveksling.serviceregistry.businesslogic.ServiceRecordPredicates.*;

@RequestMapping("/identifier")
@ExposesResourceFor(EntityResource.class)
@RestController
public class ServiceRecordController {

    private final ServiceRecordFactory serviceRecordFactory;
    private final KrrService krrService;
    private EntityService entityService;
    private PrimaryServiceStore store;
    private static final Logger logger = LoggerFactory.getLogger(ServiceRecordController.class);

    /**
     * @param virkSertService used to retrieve organization certificates
     * @param elmaLookupSerice used to lookup urls
     * @param ksLookup used for KS transport
     * @param store used to persist internal state
     * @param entityService needed to lookup and retrieve organization or citizen information using an identifier number
     * @param environment Spring environment
     * @param krrService service for kontakt og reservasjons registeret needed by DPI
     */
    @Autowired
    public ServiceRecordController(VirkSertService virkSertService, ELMALookupService elmaLookupSerice, KSLookup ksLookup, PrimaryServiceStore store, EntityService entityService, Environment environment, KrrService krrService) {
        this.entityService = entityService;
        this.store = store;
        this.krrService = krrService;
        this.serviceRecordFactory = new ServiceRecordFactory(environment, virkSertService, elmaLookupSerice, ksLookup, this.krrService);
    }

    /**
     * Used to retrieve information needed to send a message to an entity (organization or a person) having the provided
     * identifier
     * @param identifier of the organization/person to receive a message
     * @return JSON object with information needed to send a message
     */
    @RequestMapping("/{identifier}")
    @ResponseBody
    public ResponseEntity entity(@PathVariable("identifier") String identifier) {
        MDC.put("identifier", identifier);
        Entity entity = new Entity();
        ServiceIdentifier serviceIdentifier = store.getPrimaryOverride(identifier);
        EntityInfo entityInfo = entityService.getEntityInfo(identifier);
        if (entityInfo == null) {
            throw new EntityNotFoundException("Could not find entity for identifier: "+identifier);
        }
        entityInfo.setPrimaryServiceIdentifier(serviceIdentifier);

        if (usesSikkerDigitalPost().test(entityInfo)) {
            entity.addServiceRecord(serviceRecordFactory.createSikkerDigitalPostRecord(identifier));
        }
        if(usesFormidlingstjenesten().test(entityInfo)) {
            entity.addServiceRecord(serviceRecordFactory.createEduServiceRecord(identifier));
        }
        if(usesPostTilVirksomhet().test(entityInfo)) {
            entity.addServiceRecord(serviceRecordFactory.createPostVirksomhetServiceRecord(identifier));
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


    @RequestMapping("/primary")
    public ResponseEntity setPrimary(@RequestParam("orgnr") String orgnr, @RequestParam("serviceidentifier") ServiceIdentifier serviceIdentifier) {
        store.setPrimaryOverride(orgnr, serviceIdentifier);
        return new ResponseEntity(HttpStatus.OK);
    }
}

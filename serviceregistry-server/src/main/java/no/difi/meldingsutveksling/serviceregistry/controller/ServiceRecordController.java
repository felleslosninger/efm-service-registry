package no.difi.meldingsutveksling.serviceregistry.controller;


import no.difi.meldingsutveksling.serviceregistry.CertificateNotFoundException;
import no.difi.meldingsutveksling.serviceregistry.exceptions.EndpointUrlNotFound;
import no.difi.meldingsutveksling.serviceregistry.model.Organization;
import no.difi.meldingsutveksling.serviceregistry.model.OrganizationInfo;
import no.difi.meldingsutveksling.serviceregistry.model.ServiceIdentifier;
import no.difi.meldingsutveksling.serviceregistry.service.brreg.BrregService;
import no.difi.meldingsutveksling.serviceregistry.service.elma.ELMALookupService;
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

import static no.difi.meldingsutveksling.serviceregistry.businesslogic.ServiceRecordPredicates.usesFormidlingstjenesten;
import static no.difi.meldingsutveksling.serviceregistry.businesslogic.ServiceRecordPredicates.usesPostTilVirksomhet;

@RequestMapping("/identifier")
@ExposesResourceFor(OrganizationResource.class)
@RestController
public class ServiceRecordController {

    private final ServiceRecordFactory serviceRecordFactory;
    private BrregService brregService;
    private PrimaryServiceStore store;
    private static final Logger logger = LoggerFactory.getLogger(ServiceRecordController.class);

    /**
     *
     * @param virkSertService used to retrieve organization certificates
     * @param elmaLookupSerice used to lookup urls
     * @param ksLookup used for KS transport
     * @param store used to persist internal state
     * @param brregService needed to lookup and retrieve organization information using an organization number
     */
    @Autowired
    public ServiceRecordController(VirkSertService virkSertService, ELMALookupService elmaLookupSerice, KSLookup ksLookup, PrimaryServiceStore store, BrregService brregService, Environment environment) {
        this.brregService = brregService;
        this.store = store;
        this.serviceRecordFactory = new ServiceRecordFactory(environment, virkSertService, elmaLookupSerice, ksLookup);
    }

    /**
     * Used to retrieve information needed to send a message to an entity (organization or a person) having the provided
     * identifier
     * @param identifier of the organization/person to receive a message
     * @return JSON object with information needed to send a message
     */
    @RequestMapping("/{identifier}")
    @ResponseBody
    public HttpEntity<OrganizationResource> entity(@PathVariable("identifier") String identifier) {
        MDC.put("identifier", identifier);
        Organization org = new Organization();
        ServiceIdentifier serviceIdentifier = store.getPrimaryOverride(identifier);
        OrganizationInfo organization = brregService.getOrganizationInfo(identifier);
        organization.setPrimaryServiceIdentifier(serviceIdentifier);

        if(usesFormidlingstjenesten().test(organization)) {
            org.addServiceRecord(serviceRecordFactory.createEduServiceRecord(identifier));
        }
        if(usesPostTilVirksomhet().test(organization)) {
            org.addServiceRecord(serviceRecordFactory.createPostVirksomhetServiceRecord(identifier));
        }
        org.setInfo(organization);
        OrganizationResource organizationRes = new OrganizationResource(org);
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


    @RequestMapping("/primary")
    public ResponseEntity setPrimary(@RequestParam("orgnr") String orgnr, @RequestParam("serviceidentifier") String serviceIdentifier) {
        store.setPrimaryOverride(orgnr, ServiceIdentifier.valueOf(serviceIdentifier));
        return new ResponseEntity(HttpStatus.OK);
    }
}

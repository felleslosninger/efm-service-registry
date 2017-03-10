package no.difi.meldingsutveksling.serviceregistry.service;

import no.difi.meldingsutveksling.serviceregistry.model.CitizenInfo;
import no.difi.meldingsutveksling.serviceregistry.model.EntityInfo;
import no.difi.meldingsutveksling.serviceregistry.service.brreg.BrregService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static no.difi.meldingsutveksling.serviceregistry.businesslogic.ServiceRecordPredicates.isCitizen;

/**
 * Service is used to lookup information needed to send messages to an entity
 * An entity can be a citizen or an organization
 */
@Service
public class EntityService {

    private final BrregService brregService;

    @Autowired
    public EntityService(BrregService brregService) {
        this.brregService = brregService;
    }

    /**
     *
     * @param identifier for an entity either an organization number or a fodselsnummer
     * @return info needed to send messages to the entity
     */
    public EntityInfo getEntityInfo(String identifier) {
        if (isCitizen().test(identifier)) {
            return new CitizenInfo(identifier);
        } else {
            return brregService.getOrganizationInfo(identifier);
        }
    }
}

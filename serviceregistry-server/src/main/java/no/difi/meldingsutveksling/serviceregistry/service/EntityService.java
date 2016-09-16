package no.difi.meldingsutveksling.serviceregistry.service;

import no.difi.meldingsutveksling.serviceregistry.model.OrganizationInfo;
import no.difi.meldingsutveksling.serviceregistry.service.brreg.BrregService;
import no.difi.meldingsutveksling.serviceregistry.service.krr.KrrService;
import org.springframework.stereotype.Service;

import static no.difi.meldingsutveksling.serviceregistry.businesslogic.ServiceRecordPredicates.usesSikkerDigitalPost;

@Service
public class EntityService {

    private final BrregService brregService;
    private final KrrService krrService;

    public EntityService(BrregService brregService, KrrService krrService) {
        this.brregService = brregService;
        this.krrService = krrService;
    }

    public OrganizationInfo getEntityInfo(String identifier) {
        if (usesSikkerDigitalPost().test(identifier)) {
            return krrService.getCitizenInfo(identifier);
        } else {
            return brregService.getOrganizationInfo(identifier);
        }
    }
}

package no.difi.meldingsutveksling.serviceregistry.service.brreg;

import no.difi.meldingsutveksling.serviceregistry.client.brreg.BrregClient;
import no.difi.meldingsutveksling.serviceregistry.model.BrregEnhet;
import no.difi.meldingsutveksling.serviceregistry.model.OrganizationInfo;
import no.difi.meldingsutveksling.serviceregistry.model.OrganizationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class BrregService {

    private static final Logger log = LoggerFactory.getLogger(BrregService.class);

    private BrregClient brregClient;

    @Autowired
    public BrregService(BrregClient brregClient) {
        this.brregClient= brregClient;
    }

    public OrganizationInfo getOrganizationInfo(String orgNr) {
        Optional<BrregEnhet> enhet = brregClient.getBrregEnhetByOrgnr(orgNr);
        if (!enhet.isPresent()) return null;

        return enhet.map(x -> new OrganizationInfo(orgNr, x.getNavn(), OrganizationType.from(x.getOrganisasjonsform()))).orElse(new OrganizationInfo());
    }
}

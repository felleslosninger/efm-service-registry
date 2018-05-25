package no.difi.meldingsutveksling.serviceregistry.service.brreg;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import no.difi.meldingsutveksling.serviceregistry.client.brreg.BrregClient;
import no.difi.meldingsutveksling.serviceregistry.model.BrregEnhet;
import no.difi.meldingsutveksling.serviceregistry.model.EntityInfo;
import no.difi.meldingsutveksling.serviceregistry.model.OrganizationInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class BrregService {

    private BrregClient brregClient;

    private DatahotellClient datahotellClient;

    @Autowired
    public BrregService(BrregClient brregClient,
                        DatahotellClient datahotellClient) {
        this.brregClient = brregClient;
        this.datahotellClient = datahotellClient;
    }

    @HystrixCommand(
            fallbackMethod = "getOrgInfoFromDatahotell"
    )
    public Optional<EntityInfo> getOrganizationInfo(String orgnr) throws BrregNotFoundException {
        Optional<BrregEnhet> entity = brregClient.getBrregEnhetByOrgnr(orgnr);
        if (!entity.isPresent()) {
            entity = brregClient.getBrregUnderenhetByOrgnr(orgnr);
        }
        if (!entity.isPresent()) {
            throw new BrregNotFoundException(String.format("Identifier %s not found in brreg", orgnr));
        }

        return entity.map(OrganizationInfo::of);
    }

    public Optional<EntityInfo> getOrgInfoFromDatahotell(String orgnr) throws BrregNotFoundException {
        return datahotellClient.getOrganizationInfo(orgnr);
    }
}

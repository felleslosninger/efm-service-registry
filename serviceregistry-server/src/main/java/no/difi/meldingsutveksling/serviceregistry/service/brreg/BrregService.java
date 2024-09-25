package no.difi.meldingsutveksling.serviceregistry.service.brreg;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.serviceregistry.client.brreg.BrregClient;
import no.difi.meldingsutveksling.serviceregistry.domain.BrregEnhet;
import no.difi.meldingsutveksling.serviceregistry.domain.EntityInfo;
import no.difi.meldingsutveksling.serviceregistry.domain.OrganizationInfo;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class BrregService {

    private final BrregClient brregClient;

    public Optional<EntityInfo> getOrganizationInfo(String orgnr) throws BrregNotFoundException {
        Optional<BrregEnhet> entity = brregClient.getBrregEnhetByOrgnr(orgnr);
        if (entity.isEmpty()) {
            entity = brregClient.getBrregUnderenhetByOrgnr(orgnr);
        }

        return entity.map(OrganizationInfo::of);
    }

}

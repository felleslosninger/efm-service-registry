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
    private final DatahotellClient datahotellClient;

    public Optional<EntityInfo> getOrganizationInfo(String orgnr) throws BrregNotFoundException {
        log.trace("getOrganizationInfo from brreg, orgnr={}", orgnr);
        Optional<BrregEnhet> entity = brregClient.getBrregEnhetByOrgnr(orgnr);
        if (entity.isEmpty()) {
            log.debug("Looking for sub entity information");
            entity = brregClient.getBrregUnderenhetByOrgnr(orgnr);
        }
        if (entity.isEmpty()) {
            log.debug("No information from Brreg, fallback to datahotell");
            return datahotellClient.getOrganizationInfo(orgnr);
        }

        return entity.map(OrganizationInfo::of);
    }

}

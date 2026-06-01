
package no.difi.meldingsutveksling.serviceregistry.service.brreg;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.domain.Iso6523;
import no.difi.meldingsutveksling.serviceregistry.CacheConfig;
import no.difi.meldingsutveksling.serviceregistry.client.brreg.BrregClient;
import no.difi.meldingsutveksling.serviceregistry.domain.BrregEnhet;
import no.difi.meldingsutveksling.serviceregistry.domain.EntityInfo;
import no.difi.meldingsutveksling.serviceregistry.domain.OrganizationInfo;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BrregService {

    private final BrregClient brregClient;

    @Cacheable(CacheConfig.BRREG_CACHE)
    public Optional<EntityInfo> getOrganizationInfo(Iso6523 orgnr) throws BrregNotFoundException {
        Optional<BrregEnhet> entity = brregClient.getBrregEnhetByOrgnr(orgnr);
        if (entity.isEmpty()) {
            entity = brregClient.getBrregUnderenhetByOrgnr(orgnr);
        }

        return entity.map(OrganizationInfo::of);
    }

}

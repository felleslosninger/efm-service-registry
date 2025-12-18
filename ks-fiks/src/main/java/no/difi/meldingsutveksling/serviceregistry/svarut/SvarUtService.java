package no.difi.meldingsutveksling.serviceregistry.svarut;

import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.serviceregistry.CacheConfig;
import no.difi.meldingsutveksling.serviceregistry.svarut.mottakersystem.Mottakersystem;
import no.difi.meldingsutveksling.serviceregistry.svarut.mottakersystem.Mottakersystemer;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.Optional;

import static com.google.common.base.Strings.isNullOrEmpty;

@Component
@RequiredArgsConstructor
@Slf4j
public class SvarUtService {

    private final SvarUtClient svarUtClient;

    @Cacheable(CacheConfig.SVARUT_CACHE)
    @Timed(value = "svarut.client.timer", description = "Timer for Svarut RetrieveMottakerSystemForOrgnr")
    public Optional<Integer> hasSvarUtAdressering(String orgnr, Integer securityLevel) throws SvarUtClientException {
        Mottakersystemer system = svarUtClient.retrieveMottakerSystemForOrgnr(orgnr);
        var validFiksRequests = system.getMottakersystemer().stream().filter(m -> isNullOrEmpty(m.getForsendelseType()));

        if (null != securityLevel) {
            return validFiksRequests
                    .anyMatch(m -> securityLevel.equals(m.getNiva()))
                    ? Optional.of(securityLevel)
                    : Optional.empty();
        }
        return validFiksRequests
                .map(Mottakersystem::getNiva)
                .max(Comparator.naturalOrder());
    }
}

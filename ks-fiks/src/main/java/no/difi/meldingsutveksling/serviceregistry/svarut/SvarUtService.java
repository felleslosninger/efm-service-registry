package no.difi.meldingsutveksling.serviceregistry.svarut;

import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.domain.Iso6523;
import no.difi.meldingsutveksling.serviceregistry.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;

import static com.google.common.base.Strings.isNullOrEmpty;

@Component
@RequiredArgsConstructor
@Slf4j
public class SvarUtService {

    private final SvarUtClient svarUtClient;

    @Cacheable(CacheConfig.SVARUT_CACHE)
    @Timed(value = "svarut.client.timer", description = "Timer for Svarut RetrieveMottakerSystemForOrgnr")
    public Optional<Integer> hasSvarUtAdressering(Iso6523 identifier, Integer securityLevel) throws SvarUtClientException {
        RetrieveMottakerSystemForOrgnr request = RetrieveMottakerSystemForOrgnr.builder().withOrganisasjonsnr(identifier.getOrganizationIdentifier()).build();
        RetrieveMottakerSystemForOrgnrResponse response = svarUtClient.retrieveMottakerSystemForOrgnr(request);
        Stream<MottakerForsendelseTyper> validFiksRequests = response.getReturn().stream()
                .filter(m -> isNullOrEmpty(m.forsendelseType));
        if (null != securityLevel) {
            return validFiksRequests
                    .anyMatch(m -> securityLevel == m.niva)
                    ? Optional.of(securityLevel)
                    : Optional.empty();
        }
        return validFiksRequests
                .map(t -> t.niva)
                .max(Comparator.naturalOrder());
    }
}

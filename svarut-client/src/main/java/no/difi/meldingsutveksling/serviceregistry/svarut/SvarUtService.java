package no.difi.meldingsutveksling.serviceregistry.svarut;

import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.serviceregistry.CacheConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;

import static com.google.common.base.Strings.isNullOrEmpty;

@Component
@Slf4j
public class SvarUtService {

    private SvarUtClient svarUtClient;

    @Autowired
    SvarUtService(SvarUtClient svarUtClient) {
        this.svarUtClient = svarUtClient;
    }

    @Cacheable(CacheConfig.SVARUT_CACHE)
    public Optional<Integer> hasSvarUtAdressering(String orgnr, Integer securityLevel) throws SvarUtClientException {
        RetrieveMottakerSystemForOrgnr request = RetrieveMottakerSystemForOrgnr.builder().withOrganisasjonsnr(orgnr).build();
        RetrieveMottakerSystemForOrgnrResponse response;
        response = svarUtClient.retrieveMottakerSystemForOrgnr(request);
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

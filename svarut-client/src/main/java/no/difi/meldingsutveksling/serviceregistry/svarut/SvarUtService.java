package no.difi.meldingsutveksling.serviceregistry.svarut;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.Optional;

import static com.google.common.base.Strings.isNullOrEmpty;

@Component
@Slf4j
public class SvarUtService {

    private SvarUtClient svarUtClient;

    @Autowired
    SvarUtService(SvarUtClient svarUtClient) {
        this.svarUtClient = svarUtClient;
    }

    public Optional<Integer> hasSvarUtAdressering(String orgnr) {
        RetrieveMottakerSystemForOrgnr request = RetrieveMottakerSystemForOrgnr.builder().withOrganisasjonsnr(orgnr).build();
        RetrieveMottakerSystemForOrgnrResponse response = svarUtClient.retrieveMottakerSystemForOrgnr(request);
        return response.getReturn().stream()
                .filter(m -> isNullOrEmpty(m.forsendelseType))
                .map(t -> t.niva)
                .max(Comparator.naturalOrder());
    }
}

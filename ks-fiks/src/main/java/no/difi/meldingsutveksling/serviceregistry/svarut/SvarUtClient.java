package no.difi.meldingsutveksling.serviceregistry.svarut;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.serviceregistry.config.ServiceregistryProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import no.difi.meldingsutveksling.serviceregistry.svarut.mottakersystem.Mottakersystemer;

@Slf4j
@Component
@RequiredArgsConstructor
public class SvarUtClient {

    private final ServiceregistryProperties props;
    private final TokenService tokenProducer;

    private final RestClient restClient = RestClient.builder()
            .build();

    public Mottakersystemer retrieveMottakerSystemForOrgnr(String orgnr) throws SvarUtClientException{
        try {
            var accessToken = tokenProducer.fetchToken();

            return restClient.get()
                    .uri(props.getFiks().getSvarut().getBaseUrl() + "/mottakersystem?organisasjonsnummer={orgnr}", orgnr)
                    .header("Authorization", "Bearer " + accessToken)
                    .header("IntegrasjonId", props.getFiks().getSvarut().getIntegrasjonId())
                    .header("IntegrasjonPassord", props.getFiks().getSvarut().getIntegrasjonPassord())
                    .header("Accept", "application/json")
                    .retrieve()
                    .body(Mottakersystemer.class);
        } catch (Exception e) {
            throw new SvarUtClientException(e);
        }
    }
}

package no.difi.meldingsutveksling.serviceregistry.freg.mock;

import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.serviceregistry.config.ServiceregistryProperties;
import no.difi.meldingsutveksling.serviceregistry.freg.client.DefaultFregGatewayClient;
import no.difi.meldingsutveksling.serviceregistry.freg.domain.FregGatewayEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@Slf4j
@Profile({"dev-local", "dev", "test", "yt"})
public class DefaultFregClientWithMock extends DefaultFregGatewayClient {
    @Autowired
    @Qualifier("fregGatewayRestTemplate")
    private RestTemplate restTemplate;

    public DefaultFregClientWithMock(ServiceregistryProperties properties) {
        super(properties);
    }

    @Override
    public Optional<FregGatewayEntity.Address.Response> getPersonAdress(String pid) {
        try {
            String url = properties.getFreg().getEndpointURL() + "person/personadresse/" + pid;
            ResponseEntity<FregGatewayEntity.Address.Response> response = restTemplate.getForEntity(
                    url,
                    FregGatewayEntity.Address.Response.class,
                    pid);
            FregGatewayEntity.Address.Response responseBody = response.getBody();
            return Optional.of(responseBody);
        } catch (HttpClientErrorException.NotFound e) {
            log.info("User {} not found in Tenor testdatasøk. Returning mock user", pid);
            return Optional.of(FregGatewayEntity.Address.Response.builder()
                    .personIdentifikator(pid)
                    .navn(FregGatewayEntity.Address.Navn.builder()
                            .fornavn("Raff")
                            .etternavn("Raffen")
                            .build())
                    .postadresse(FregGatewayEntity.Address.PostAdresse.builder()
                            .adresselinje(new ArrayList<>(List.of("Portveien 2")))
                            .postnummer("0468")
                            .poststed("Oslo")
                            .landkode("Norge")
                            .build())
                .build());
        }
    }
}
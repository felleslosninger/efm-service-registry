package no.difi.meldingsutveksling.serviceregistry.freg.client;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.serviceregistry.config.ServiceregistryProperties;
import no.difi.meldingsutveksling.serviceregistry.freg.domain.FregGatewayEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;


@Component
@RequiredArgsConstructor
@Profile("production")
public class DefaultFregGatewayClient implements FregGatewayClient {
    private final ServiceregistryProperties properties;

    @Autowired
    @Qualifier("fregGatewayRestTemplate")
    private RestTemplate restTemplate;

    @Override
    public Optional<FregGatewayEntity.Address.Response> getPersonAdress(String pid) {
            ResponseEntity<FregGatewayEntity.Address.Response> response = restTemplate.getForEntity(
                    properties.getFreg().getEndpointURL() + "person/personadresse/{pid}",
                    FregGatewayEntity.Address.Response.class, pid);
            return Optional.of(response.getBody());
    }
}

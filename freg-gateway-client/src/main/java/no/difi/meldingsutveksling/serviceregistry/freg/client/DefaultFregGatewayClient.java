package no.difi.meldingsutveksling.serviceregistry.freg.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.serviceregistry.config.ServiceregistryProperties;
import no.difi.meldingsutveksling.serviceregistry.freg.domain.FregGatewayEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URISyntaxException;
import java.util.Optional;


@Slf4j
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
        try {
            return getAddressFromFreg(pid);
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        } catch (URISyntaxException e) {
            log.debug("Invalid URI encountered in Folkeregisteret client", e);
            return Optional.empty();
        }

    }

    protected Optional<FregGatewayEntity.Address.Response> getAddressFromFreg(String pid) throws HttpClientErrorException, URISyntaxException {
        String url = UriComponentsBuilder.fromPath(properties.getFreg().getEndpointURL())
                .pathSegment("person")
                .pathSegment("personadresse")
                .pathSegment(pid)
                .build().toUriString();
        ResponseEntity<FregGatewayEntity.Address.Response> response = restTemplate.getForEntity(
                url,
                FregGatewayEntity.Address.Response.class,
                pid);
        FregGatewayEntity.Address.Response responseBody = response.getBody();
        return Optional.of(responseBody);
    }
}



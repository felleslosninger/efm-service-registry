package no.difi.meldingsutveksling.serviceregistry.freg.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.serviceregistry.config.ServiceregistryProperties;
import no.difi.meldingsutveksling.serviceregistry.freg.domain.FregGatewayEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
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
@ConditionalOnProperty(prefix = "difi.move.freg", name = "enabled", havingValue = "true")
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
        String url = UriComponentsBuilder.fromUriString(properties.getFreg().getEndpointURL())
                .pathSegment("person")
                .pathSegment("personadresse")
                .pathSegment(pid)
                .build().toUriString();
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-API-Key", properties.getFreg().getApiKey());

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<FregGatewayEntity.Address.Response> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                FregGatewayEntity.Address.Response.class
        );
        FregGatewayEntity.Address.Response responseBody = response.getBody();
        return Optional.of(responseBody);
    }
}



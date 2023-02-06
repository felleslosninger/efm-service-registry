package no.difi.meldingsutveksling.serviceregistry.freg.client;

import no.difi.meldingsutveksling.serviceregistry.config.ServiceregistryProperties;
import no.difi.meldingsutveksling.serviceregistry.freg.exception.NotFoundInMfGatewayException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import no.difi.meldingsutveksling.serviceregistry.freg.domain.FregGatewayEntity;

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
    public Optional<FregGatewayEntity.Address.Response> getPersonAdress(String pid) throws NotFoundInMfGatewayException {
        try {
            ResponseEntity<FregGatewayEntity.Address.Response> response = restTemplate.getForEntity(
                    properties.getFreg().getEndpointURL() + "person/personadresse/{pid}",
                    FregGatewayEntity.Address.Response.class, pid);
            return Optional.of(response.getBody());
        } catch (HttpClientErrorException httpException) {
            if (httpException.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new NotFoundInMfGatewayException(httpException);
            }

        }
        return null;
    }
}

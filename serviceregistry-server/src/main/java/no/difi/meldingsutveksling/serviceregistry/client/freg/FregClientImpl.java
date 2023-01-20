package no.difi.meldingsutveksling.serviceregistry.client.freg;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.serviceregistry.config.ServiceregistryProperties;
import no.difi.meldingsutveksling.serviceregistry.krr.*;
import no.difi.move.common.oauth.JwtTokenClient;
import no.difi.move.common.oauth.JwtTokenConfig;
import no.difi.move.common.oauth.JwtTokenResponse;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class FregClientImpl implements FregClient {

    private final ObjectMapper objectMapper;
    private final ServiceregistryProperties properties;

    private JwtTokenClient jwtTokenClient() {
        JwtTokenConfig config = new JwtTokenConfig(
                properties.getFreg().getMpClientId(),
                properties.getAuth().getMaskinportenIssuer()+ "/token",
                properties.getAuth().getMaskinportenIssuer(),
                properties.getFreg().getScopes(),
                properties.getSign().getKeystore()
        );

        return new JwtTokenClient(config);
    }


    @Override
    public Optional<FregGatewayResource> getFregPersonByPid(LookupParameters lookupParameters) throws KontaktInfoException {

        String response = fetchKontaktInfo(lookupParameters.getIdentifier(), lookupParameters.getToken().getTokenValue(), properties.getFreg().getEndpointURL());

            return mapResponse(response, FregGatewayResource.class);
    }


    ///v1/personer/:personidentifikator
    String fetchKontaktInfo(String identifier, String token, URI uri) throws KontaktInfoException {
        PersonRequest request = PersonRequest.of(identifier);

        JwtTokenClient client = jwtTokenClient();
        JwtTokenResponse maskinportenToken = client.fetchToken();

        HttpHeaders headers = new HttpHeaders();
//        headers.set("Authorization", "Bearer " + maskinportenToken.getAccessToken());
        headers.set("Accept", "application/json");
        HttpEntity<Object> httpEntity = new HttpEntity<>(headers);

        RestTemplate rt = new RestTemplate();
        String uri2 = "http://localhost:8099/";
        ResponseEntity<String> response = rt.exchange(uri2 + "person/personadresse/" + request.getPersonIdentifiers().get(0), HttpMethod.GET, httpEntity, String.class);
//        ResponseEntity<String> response = rt.exchange(uri + "v1/personer/" + request.getPersonIdentifiers().get(0), HttpMethod.GET, httpEntity, String.class);

        if (response.getStatusCode() != HttpStatus.OK) {
            throw new KontaktInfoException(String.format("%s endpoint returned %s (%s)", uri, response.getStatusCode().value(),
                    response.getStatusCode().getReasonPhrase()));
        }

        return response.getBody();
    }

    private <T> Optional<T> mapResponse(String response, Class<T> clazz) throws KontaktInfoException {
        FregGatewayResponse<T> mappedResponse;
        try {
            mappedResponse = objectMapper.readValue(response,
                    objectMapper.getTypeFactory().constructParametricType(FregGatewayResponse.class, clazz));

        } catch (IOException e) {
            throw new KontaktInfoException("Error mapping payload to " + FregGatewayResponse.class.getName(), e);
        }

        if (mappedResponse.getPersonList() == null || mappedResponse.getPersonList().isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(mappedResponse.getPersonList().get(0));
    }
}

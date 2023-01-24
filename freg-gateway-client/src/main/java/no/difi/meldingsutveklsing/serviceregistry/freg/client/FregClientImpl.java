package no.difi.meldingsutveklsing.serviceregistry.freg.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveklsing.serviceregistry.freg.domain.Person;
import no.difi.meldingsutveklsing.serviceregistry.freg.exception.FregGatewayException;
import no.difi.meldingsutveksling.serviceregistry.config.ServiceregistryProperties;
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

    public Optional<Person> getFregPersonByPid(String pid) throws FregGatewayException {

        String response = fetchKontaktInfo(pid, properties.getFreg().getEndpointURL());

            return mapResponse(response, Person.class);
    }


    ///v1/personer/:personidentifikator
    String fetchKontaktInfo(String identifier, URI uri) throws FregGatewayException{

        HttpHeaders headers = new HttpHeaders();
//        headers.set("Accept", "application/json");
        HttpEntity<Object> httpEntity = new HttpEntity<>(headers);

        RestTemplate rt = new RestTemplate();
        String uri2 = "http://localhost:8099/";
        ResponseEntity<String> response = rt.exchange(uri + "person/personadresse/" + identifier, HttpMethod.GET, httpEntity, String.class);
//        ResponseEntity<String> response = rt.exchange(uri + "v1/personer/" + request.getPersonIdentifiers().get(0), HttpMethod.GET, httpEntity, String.class);

        if (response.getStatusCode() != HttpStatus.OK) {
            throw new FregGatewayException(String.format("%s endpoint returned %s (%s)", uri, response.getStatusCode().value(),
                    response.getStatusCode().getReasonPhrase()));
        }

        return response.getBody();
    }

    private <T> Optional<T> mapResponse(String response, Class<T> clazz) throws FregGatewayException {
//        FregGatewayResponse<T> mappedResponse;
//        try {
//            mappedResponse = objectMapper.readValue(response,
//                    objectMapper.getTypeFactory().constructParametricType(Person.class, clazz));
//
//        } catch (IOException e) {
//            throw new FregGatewayException("Error mapping payload to " + Person.class.getName(), e);
//        }
//
//        if (mappedResponse.getPersonList() == null || mappedResponse.getPersonList().isEmpty()) {
//            return Optional.empty();
//        }
//
//        return Optional.of(mappedResponse.getPersonList().get(0));
        return null;
    }
}

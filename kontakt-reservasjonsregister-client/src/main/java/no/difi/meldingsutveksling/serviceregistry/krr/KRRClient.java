package no.difi.meldingsutveksling.serviceregistry.krr;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;

@Component
@RequiredArgsConstructor
public class KRRClient extends KontaktInfoClient {

    private final ObjectMapper objectMapper;

    public PersonResource getPersonResource(LookupParameters params, URI endpointUri) throws KontaktInfoException {

        String response = fetchKontaktInfo(params.getIdentifier(), params.getToken().getTokenValue(), endpointUri);

        PersonerResponse personerResponse;
        try {
            personerResponse = objectMapper.readValue(response, PersonerResponse.class);
        } catch (IOException e) {
            throw new KontaktInfoException("Error mapping payload to " + PersonerResponse.class.getName(), e);
        }

        return personerResponse.getPersons().get(0);
    }
}

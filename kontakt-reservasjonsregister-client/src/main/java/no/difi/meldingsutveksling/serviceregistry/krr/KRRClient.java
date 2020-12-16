package no.difi.meldingsutveksling.serviceregistry.krr;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.net.URI;

@RequiredArgsConstructor
public class KRRClient extends KontaktInfoClient {

    private final URI endpointUri;

    public PersonResource getPersonResource(String identifier, String token) throws KontaktInfoException {

        String response = fetchKontaktInfo(identifier, token, endpointUri);

        ObjectMapper om = new ObjectMapper();
        PersonerResponse personerResponse;
        try {
            personerResponse = om.readValue(response, PersonerResponse.class);
        } catch (IOException e) {
            throw new KontaktInfoException("Error mapping payload to " + PersonerResponse.class.getName(), e);
        }

        return personerResponse.getPersons().get(0);
    }
}

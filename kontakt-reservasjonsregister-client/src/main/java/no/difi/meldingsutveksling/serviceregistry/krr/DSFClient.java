package no.difi.meldingsutveksling.serviceregistry.krr;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.net.URI;
import java.util.Optional;

@RequiredArgsConstructor
public class DSFClient extends KontaktInfoClient {

    private final URI endpointUri;

    public Optional<DSFResource> getDSFResource(String identifier, String token) throws KontaktInfoException {

        String response = fetchKontaktInfo(identifier, token, endpointUri);

        ObjectMapper om = new ObjectMapper();
        DSFResponse dsfResponse;
        try {
            dsfResponse = om.readValue(response, DSFResponse.class);
        } catch (IOException e) {
            throw new KontaktInfoException("Error mapping payload to " + DSFResponse.class.getName(), e);
        }

        if (dsfResponse.getPersons() == null || dsfResponse.getPersons().isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(dsfResponse.getPersons().get(0));
    }

}

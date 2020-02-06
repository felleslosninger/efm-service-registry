package no.difi.meldingsutveksling.serviceregistry.krr;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.proc.BadJWSException;
import no.difi.move.common.oauth.JWTDecoder;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.cert.CertificateException;
import java.util.Objects;
import java.util.Optional;

public class DSFClient {

    private URL endpointURL;

    public DSFClient(URL endpointURL) {
        this.endpointURL= endpointURL;
    }

    public Optional<DSFResource> getDSFResource(String identifier, String token) throws DsfLookupException {

        URI uri;
        try {
             uri = endpointURL.toURI();
        } catch (URISyntaxException e) {
            throw new DsfLookupException("Failed to create URI instance of \"" + endpointURL + "\"", e);
        }

        PersonRequest request = PersonRequest.of(identifier);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer "+token);
        headers.set("Accept", "application/jose");
        HttpEntity<Object> httpEntity = new HttpEntity<>(request, headers);

        RestTemplate rt = new RestTemplate();
        ResponseEntity<String> response = rt.exchange(uri, HttpMethod.POST, httpEntity, String.class);

        if (response.getStatusCode() != HttpStatus.OK) {
            throw new DsfLookupException(String.format("DSF endpoint returned %s (%s)", response.getStatusCode().value(),
                    response.getStatusCode().getReasonPhrase()));
        }

        String payload;
        try {
            JWTDecoder jwtDecoder = new JWTDecoder();
            payload = jwtDecoder.getPayload(Objects.requireNonNull(response.getBody()));
        } catch (CertificateException | BadJWSException e) {
            throw new DsfLookupException("Error during decoding JWT response from DSF" ,e);
        }

        ObjectMapper om = new ObjectMapper();
        DSFResponse dsfResponse;
        try {
            dsfResponse = om.readValue(payload, DSFResponse.class);
        } catch (IOException e) {
            throw new DsfLookupException("Error mapping payload to " + DSFResponse.class.getName(), e);
        }

        if (dsfResponse.getPersons() == null || dsfResponse.getPersons().isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(dsfResponse.getPersons().get(0));
    }
}

package no.difi.meldingsutveksling.serviceregistry.krr;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.proc.BadJWSException;
import no.difi.move.common.oauth.JWTDecoder;
import no.difi.move.common.oauth.KeystoreHelper;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.cert.CertificateException;

public class KRRClient {

    private URL endpointURL;
    private KeystoreHelper keystoreHelper;

    public KRRClient(URL endpointURL, KeystoreHelper keystoreHelper) {
        this.endpointURL= endpointURL;
        this.keystoreHelper = keystoreHelper;
    }

    public PersonResource getPersonResource(String identifier, String token) throws KRRClientException {

        URI uri;
        try {
             uri = this.endpointURL.toURI();
        } catch (URISyntaxException e) {
            throw new KRRClientException("Failed to create URI instance of \"" + endpointURL + "\"", e);
        }

        PersonRequest request = PersonRequest.of(identifier);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer "+token);
        headers.set("Accept", "application/jose");
        HttpEntity<Object> httpEntity = new HttpEntity<>(request, headers);

        RestTemplate rt = new RestTemplate();
        ResponseEntity<String> response = rt.exchange(uri, HttpMethod.POST, httpEntity, String.class);
        if (response.getStatusCode() == HttpStatus.NOT_FOUND) {
            throw new KRRClientException("KRR endpoint returned 404 (Not Found) for identifier " + identifier);
        }
        if (response.getStatusCode() == HttpStatus.UNAUTHORIZED) {
            throw new KRRClientException("KRR endpoint returned 401 (Unauthorized)");
        }

        String payload;
        try {
            JWTDecoder jwtDecoder = new JWTDecoder(keystoreHelper);
            payload = jwtDecoder.getPayload(response.getBody());
        } catch (CertificateException | BadJWSException e) {
            throw new KRRClientException("Error during decoding JWT response from KRR" ,e);
        }

        ObjectMapper om = new ObjectMapper();
        PersonerResponse personerResponse;
        try {
            personerResponse = om.readValue(payload, PersonerResponse.class);
        } catch (IOException e) {
            throw new KRRClientException("Error mapping payload to " + PersonerResponse.class.getName(), e);
        }

        return personerResponse.getPersons().get(0);
    }
}

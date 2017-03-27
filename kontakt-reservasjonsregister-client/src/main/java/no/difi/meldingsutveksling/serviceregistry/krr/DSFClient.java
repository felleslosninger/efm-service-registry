package no.difi.meldingsutveksling.serviceregistry.krr;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

public class DSFClient {

    private static final Logger log = LoggerFactory.getLogger(DSFClient.class);

    private URL endpointURL;

    public DSFClient(URL endpointURL) {
        this.endpointURL= endpointURL;
    }

    public DSFResource getDSFResource(String identifier, String token) throws KRRClientException {

        URI uri;
        try {
             uri = endpointURL.toURI();
        } catch (URISyntaxException e) {
            throw new KRRClientException("Failed to create URI instance of \"" + endpointURL + "\"", e);
        }

        PersonRequest request = PersonRequest.of(identifier);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer "+token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Object> httpEntity = new HttpEntity<>(request, headers);

        RestTemplate rt = new RestTemplate();
        ResponseEntity<DSFResponse> response = rt.exchange(uri, HttpMethod.POST, httpEntity, DSFResponse.class);

        if (response.getStatusCode() == HttpStatus.NOT_FOUND) {
            throw new KRRClientException("KRR endpoint returned 404 (Not Found) for identifier " + identifier);
        }
        if (response.getStatusCode() == HttpStatus.UNAUTHORIZED) {
            throw new KRRClientException("KRR endpoint returned 401 (Unauthorized)");
        }

        return response.getBody().getPersons().get(0);
    }
}

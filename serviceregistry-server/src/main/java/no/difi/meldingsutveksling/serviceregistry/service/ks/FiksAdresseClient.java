package no.difi.meldingsutveksling.serviceregistry.service.ks;

import net.logstash.logback.marker.Markers;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import static org.springframework.http.HttpStatus.NOT_FOUND;

public class FiksAdresseClient {
    private RestTemplate restTemplate;
    private URL url;
    private Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass().getName());

    public FiksAdresseClient(RestTemplate restTemplate, URL url) {
        this.restTemplate = restTemplate;
        this.url = url;
    }

    public FiksContactInfo getOrganization(String identifier) {
        final URI orgUrl;
        try {
            URIBuilder builder;
            builder = new URIBuilder(url.toString());
            orgUrl = builder.setPath(url.getPath() + "/organization/" + identifier).build().normalize();
        } catch (URISyntaxException e) {
            throw new FiksAdresseServiceException("URL to MOVE FIKS adresse service is malformed", e);
        }
        try {
            restTemplate.getForEntity(orgUrl, FiksContactInfo.class);
        } catch (HttpClientErrorException e) {
            if (NOT_FOUND != e.getStatusCode()) {
                logger.error(Markers.append("receiver", identifier), "Failed to lookup in FIKS adresse service", e);
            }
            return FiksContactInfo.EMPTY;
        }
        return new FiksContactInfo();
    }

    public RestTemplate getRestTemplate() {
        return restTemplate;
    }
}

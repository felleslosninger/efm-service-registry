package no.difi.meldingsutveksling.serviceregistry.client.brreg;

import no.difi.meldingsutveksling.serviceregistry.model.BrregEnhet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.Collections;
import java.util.Optional;

/**
 * Client for "Enhetsregisteret" at data.brreg.no
 *
 * Created by kons-mwa on 06.06.2016.
 */
public class BrregClientImpl implements BrregClient {
    private static Logger log = LoggerFactory.getLogger(BrregClientImpl.class);

    private URI uri;

    /**
     * Creates a client configured to connect to http://data.brreg.no/
     * @param uri
     */
    public BrregClientImpl(URI uri) {
        this.uri = uri;
    }

    /**
     * Lookup an organization in BRREG
     * @param orgnr organization number to lookup
     * @return BRREG enhet or empty if none is found
     */
    @Override
    public Optional<BrregEnhet> getBrregEnhetByOrgnr(String orgnr) {
        String apiVersjon = "application/vnd.brreg.enhetsregisteret.enhet.v1+json";
        HttpEntity<BrregEnhet> entity = getEntityFromHeader(apiVersjon);

        return getEnhet("enhetsregisteret/api/enheter/", orgnr, entity);
    }

    /**
     * Lookup a sub organization in BRREG
     * @param orgnr organization number to lookup
     * @return BRREG enhet or empty if none is found
     */
    @Override
    public Optional<BrregEnhet> getBrregUnderenhetByOrgnr(String orgnr) {
        String apiVersjon = "application/vnd.brreg.enhetsregisteret.underenhet.v1+json";
        HttpEntity<BrregEnhet> entity = getEntityFromHeader(apiVersjon);

        return getEnhet("enhetsregisteret/api/underenheter/", orgnr, entity);
    }

    private Optional<BrregEnhet> getEnhet(String registerUriPart, String orgnr, HttpEntity entity) {
        URI currentURI = uri.resolve(String.format("%s/%s.json", registerUriPart, orgnr));

        RestTemplate rt = new RestTemplate();
        rt.exchange(currentURI, HttpMethod.GET, entity, BrregEnhet.class);

        try {
            ResponseEntity<BrregEnhet> response = rt.getForEntity(currentURI, BrregEnhet.class);
            if (response.getStatusCode() == HttpStatus.OK) {
                return Optional.of(response.getBody());
            }
        } catch (Exception e) {
            log.error(String.format("Error looking up entity with identifier=%s in brreg", orgnr), e);
        }
        return Optional.empty();

    }

    private HttpEntity<BrregEnhet> getEntityFromHeader(String apiVersjon) {
        HttpHeaders header = new HttpHeaders();
        header.set("Accept", apiVersjon);
        HttpEntity<BrregEnhet> entity = new HttpEntity<>(header);

        return entity;
    }
}

package no.difi.meldingsutveksling.serviceregistry.client.brreg;

import no.difi.meldingsutveksling.serviceregistry.model.BrregEnhet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.time.LocalDate;
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
        return getEnhet("enhetsregisteret/api/enheter/",
                "application/vnd.brreg.enhetsregisteret.enhet.v1+json",
                orgnr);
    }

    /**
     * Lookup a sub organization in BRREG
     * @param orgnr organization number to lookup
     * @return BRREG enhet or empty if none is found
     */
    @Override
    public Optional<BrregEnhet> getBrregUnderenhetByOrgnr(String orgnr) {
        return getEnhet("enhetsregisteret/api/underenheter/",
                "application/vnd.brreg.enhetsregisteret.underenhet.v1+json",
                orgnr);
    }

    private Optional<BrregEnhet> getEnhet(String registerUriPart, String apiVersjon, String orgnr) {
        URI currentURI = uri.resolve(String.format("%s/%s", registerUriPart, orgnr));
        RestTemplate rt = new RestTemplate();

        HttpHeaders header = new HttpHeaders();
        header.set("Accept", apiVersjon);
        HttpEntity<Object> entity = new HttpEntity<>(header);
        try {
            ResponseEntity<BrregEnhet> response = rt.exchange(currentURI, HttpMethod.GET, entity, BrregEnhet.class);
            if (response.getStatusCode() == HttpStatus.OK) {
                if (response.getBody() != null && response.getBody().getSlettedato() != null) {
                    if (response.getBody().getSlettedato().isBefore(LocalDate.now())) {
                        return Optional.empty();
                    }
                }
                return Optional.ofNullable(response.getBody());
            }
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() != HttpStatus.NOT_FOUND) {
                log.error(String.format("Error looking up entity with identifier=%s in brreg", orgnr), e);
            }
        }
        return Optional.empty();

    }

}

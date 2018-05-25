package no.difi.meldingsutveksling.serviceregistry.client.brreg;

import no.difi.meldingsutveksling.serviceregistry.model.BrregEnhet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
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
        return getEnhet("enhetsregisteret/enhet", orgnr);
    }

    /**
     * Lookup a sub organization in BRREG
     * @param orgnr organization number to lookup
     * @return BRREG enhet or empty if none is found
     */
    @Override
    public Optional<BrregEnhet> getBrregUnderenhetByOrgnr(String orgnr) {
        return getEnhet("enhetsregisteret/underenhet", orgnr);
    }

    private Optional<BrregEnhet> getEnhet(String registerUriPart, String orgnr) {
        URI currentURI = uri.resolve(String.format("%s/%s.json", registerUriPart, orgnr));

        RestTemplate rt = new RestTemplate();
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
}

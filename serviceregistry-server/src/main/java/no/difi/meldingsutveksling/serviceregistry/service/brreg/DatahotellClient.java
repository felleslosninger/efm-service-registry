package no.difi.meldingsutveksling.serviceregistry.service.brreg;

import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.serviceregistry.exceptions.ServiceRegistryException;
import no.difi.meldingsutveksling.serviceregistry.config.ServiceregistryProperties;
import no.difi.meldingsutveksling.serviceregistry.domain.EntityInfo;
import no.difi.meldingsutveksling.serviceregistry.domain.OrganizationInfo;
import no.difi.meldingsutveksling.serviceregistry.domain.DatahotellEntry;
import no.difi.meldingsutveksling.serviceregistry.domain.datahotell.DatahotellRespons;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

import static java.lang.String.format;

@Component
@Slf4j
public class DatahotellClient {

    private final ServiceregistryProperties props;

    @Autowired
    public DatahotellClient(ServiceregistryProperties props) {
        this.props = props;
    }

    public Optional<EntityInfo> getOrganizationInfo(String orgnr) throws BrregNotFoundException {
        String trimmedOrgnr = trim(orgnr);
        Optional<DatahotellEntry> enhet = getHovedenhet(trimmedOrgnr);
        if (enhet.isEmpty()) {
            enhet = getUnderenhet(trimmedOrgnr);
        }
        if (enhet.isEmpty()) {
            throw new BrregNotFoundException(String.format("Identifier %s not found in datahotell", trimmedOrgnr));
        }

        return enhet.map(OrganizationInfo::of);
    }

    protected String trim(String in) {
        return in.replaceAll("\\s+", "");
    }

    protected Optional<DatahotellEntry> getHovedenhet(String orgnr) {
        return getEnhet("api/json/brreg/enhetsregisteret?orgnr=", orgnr);
    }

    protected Optional<DatahotellEntry> getUnderenhet(String orgnr) {
        return getEnhet("api/json/brreg/underenheter?orgnr=", orgnr);
    }

    private Optional<DatahotellEntry> getEnhet(String uriPart, String orgnr) {
        URI uri;
        try {
            uri = props.getDatahotell().getEndpointURL().toURI().resolve(format("%s%s", uriPart, orgnr));
        } catch (URISyntaxException e) {
            throw new ServiceRegistryException("Error in Datahotell URI", e);
        }

        RestTemplate rt = new RestTemplate();
        try {
            ResponseEntity<DatahotellRespons> response = rt.getForEntity(uri, DatahotellRespons.class);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null && !response.getBody().getEntries().isEmpty()) {
                return Optional.of(response.getBody().getEntries().get(0));
            }
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() != HttpStatus.NOT_FOUND) {
                log.error("Error looking up {}", uri, e);
            }
        }

        return Optional.empty();
    }

}

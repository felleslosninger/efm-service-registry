package no.difi.meldingsutveksling.serviceregistry.service.brreg;

import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryException;
import no.difi.meldingsutveksling.serviceregistry.config.ServiceregistryProperties;
import no.difi.meldingsutveksling.serviceregistry.model.EntityInfo;
import no.difi.meldingsutveksling.serviceregistry.model.OrganizationInfo;
import no.difi.meldingsutveksling.serviceregistry.model.datahotell.DatahotellEntry;
import no.difi.meldingsutveksling.serviceregistry.model.datahotell.DatahotellRespons;
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

    private ServiceregistryProperties props;

    @Autowired
    public DatahotellClient(ServiceregistryProperties props) {
        this.props = props;
    }

    public Optional<EntityInfo> getOrganizationInfo(String orgnr) throws BrregNotFoundException {
        Optional<DatahotellEntry> enhet = getHovedenhet(orgnr);
        if (!enhet.isPresent()) {
            enhet = getUnderenhet(orgnr);
        }
        if (!enhet.isPresent()) {
            throw new BrregNotFoundException(String.format("Identifier %s not found in datahotell", orgnr));
        }

        return enhet.map(OrganizationInfo::of);
    }

    private Optional<DatahotellEntry> getHovedenhet(String orgnr) {
        return getEnhet("api/json/brreg/enhetsregisteret?orgnr=", orgnr);
    }

    private Optional<DatahotellEntry> getUnderenhet(String orgnr) {
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

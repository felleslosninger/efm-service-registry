package no.difi.meldingsutveksling.serviceregistry.servicerecord;

import no.difi.meldingsutveksling.serviceregistry.config.ServiceregistryProperties;
import static no.difi.meldingsutveksling.serviceregistry.model.ServiceIdentifier.POST_VIRKSOMHET;
import org.springframework.beans.factory.annotation.Autowired;

public class PostVirksomhetServiceRecord extends ServiceRecord {

    @Autowired
    public PostVirksomhetServiceRecord(ServiceregistryProperties properties, String orgnr) {
        super(properties, null, POST_VIRKSOMHET, orgnr);
    }

    @Override
    public String getEndPointURL() {
        return properties.getDpv().getEndpointURL().toString();
    }
}

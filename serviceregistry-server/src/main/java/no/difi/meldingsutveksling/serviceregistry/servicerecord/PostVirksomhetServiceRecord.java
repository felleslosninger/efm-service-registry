package no.difi.meldingsutveksling.serviceregistry.servicerecord;

import no.difi.meldingsutveksling.serviceregistry.config.ServiceregistryProperties;
import org.springframework.beans.factory.annotation.Autowired;

import static no.difi.meldingsutveksling.serviceregistry.model.ServiceIdentifier.DPV;

public class PostVirksomhetServiceRecord extends ServiceRecord {

    @Autowired
    public PostVirksomhetServiceRecord(ServiceregistryProperties properties, String orgnr) {
        super(properties, null, DPV, orgnr);
    }

    @Override
    public String getEndPointURL() {
        return properties.getDpv().getEndpointURL().toString();
    }
}

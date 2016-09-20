package no.difi.meldingsutveksling.serviceregistry.servicerecord;

import no.difi.meldingsutveksling.serviceregistry.model.ServiceIdentifier;
import org.springframework.core.env.Environment;

public class SikkerDigitalPostServiceRecord extends ServiceRecord {
    public SikkerDigitalPostServiceRecord(Environment e, String pemCertificate, ServiceIdentifier serviceIdentifier, String organisationNumber) {
        super(e, pemCertificate, serviceIdentifier, organisationNumber);
    }

    @Override
    public String getEndPointURL() {
        return null;
    }
}

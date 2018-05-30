package no.difi.meldingsutveksling.serviceregistry.servicerecord;

import no.difi.meldingsutveksling.serviceregistry.model.ServiceIdentifier;

public class ErrorServiceRecord extends ServiceRecord {

    public ErrorServiceRecord(String pemCertificate, ServiceIdentifier serviceIdentifier, String organisationNumber) {
        super(pemCertificate, serviceIdentifier, organisationNumber);
    }

    @Override
    public String getEndPointURL() {
        return null;
    }

    public static ErrorServiceRecord create(ServiceIdentifier serviceIdentifier) {
        return new ErrorServiceRecord(null, serviceIdentifier, null);
    }
}

package no.difi.meldingsutveksling.serviceregistry.servicerecord;

import no.difi.meldingsutveksling.serviceregistry.model.ServiceIdentifier;

public class ErrorServiceRecord extends ServiceRecord {

    public ErrorServiceRecord(ServiceIdentifier serviceIdentifier) {
        super(serviceIdentifier, null, null);
    }

    public static ErrorServiceRecord of(ServiceIdentifier serviceIdentifier) {
        return new ErrorServiceRecord(serviceIdentifier);
    }
}

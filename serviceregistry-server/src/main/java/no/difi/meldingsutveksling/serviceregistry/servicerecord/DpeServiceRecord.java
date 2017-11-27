package no.difi.meldingsutveksling.serviceregistry.servicerecord;

import no.difi.meldingsutveksling.serviceregistry.model.ServiceIdentifier;

public class DpeServiceRecord extends ServiceRecord {

    private DpeServiceRecord(String pemCertificate, ServiceIdentifier serviceIdentifier, String organisationNumber) {
        super(pemCertificate, serviceIdentifier, organisationNumber);
    }

    public static DpeServiceRecord of(String pemCertificate, String organizationNumber, ServiceIdentifier serviceIdentifier) {
        DpeServiceRecord dpeServiceRecord = new DpeServiceRecord(pemCertificate, serviceIdentifier, organizationNumber);
        dpeServiceRecord.addDpeCapability(serviceIdentifier.toString());
        return dpeServiceRecord;
    }

    @Override
    public String getEndPointURL() {
        return null;
    }
}

package no.difi.meldingsutveksling.serviceregistry.record;

import no.difi.meldingsutveksling.serviceregistry.domain.ServiceIdentifier;

public class DpeServiceRecord extends ServiceRecord {

    private DpeServiceRecord(ServiceIdentifier serviceIdentifier, String organisationNumber, String queue) {
        super(serviceIdentifier, organisationNumber, queue);
    }

    public static DpeServiceRecord of(String pemCertificate, String organizationNumber, ServiceIdentifier serviceIdentifier, String queue) {
        DpeServiceRecord dpeServiceRecord = new DpeServiceRecord(serviceIdentifier, organizationNumber, queue);
        dpeServiceRecord.setPemCertificate(pemCertificate);
        return dpeServiceRecord;
    }

}
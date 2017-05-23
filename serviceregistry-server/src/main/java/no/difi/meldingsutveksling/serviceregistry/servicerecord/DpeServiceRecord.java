package no.difi.meldingsutveksling.serviceregistry.servicerecord;

import no.difi.meldingsutveksling.serviceregistry.model.ServiceIdentifier;

public class DpeServiceRecord extends ServiceRecord {

    private DpeServiceRecord(String pemCertificate, ServiceIdentifier serviceIdentifier, String organisationNumber) {
        super(pemCertificate, serviceIdentifier, organisationNumber);
    }

    public static DpeServiceRecord of(String pemCertificate, String organizationNumber) {
        DpeServiceRecord dpeServiceRecord = new DpeServiceRecord(pemCertificate, ServiceIdentifier.DPE_INNSYN, organizationNumber);
        dpeServiceRecord.addDpeCapability(ServiceIdentifier.DPE_INNSYN.toString());
        return dpeServiceRecord;
    }

    @Override
    public String getEndPointURL() {
        return null;
    }
}

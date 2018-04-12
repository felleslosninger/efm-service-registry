package no.difi.meldingsutveksling.serviceregistry.servicerecord;

import no.difi.meldingsutveksling.serviceregistry.model.ServiceIdentifier;

public class FiksServiceRecord extends ServiceRecord {
    public FiksServiceRecord(String orgnr, String pemCertificate, String endpointUrl) {
        super(pemCertificate, ServiceIdentifier.DPF, orgnr);
        this.endpointUrl = endpointUrl;
    }

    @Override
    public String getEndPointURL() {
        return endpointUrl;
    }
}

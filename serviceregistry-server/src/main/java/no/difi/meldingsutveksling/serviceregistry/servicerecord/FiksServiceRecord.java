package no.difi.meldingsutveksling.serviceregistry.servicerecord;

import no.difi.meldingsutveksling.serviceregistry.model.ServiceIdentifier;
import no.difi.meldingsutveksling.serviceregistry.service.ks.FiksAdressing;

public class FiksServiceRecord extends ServiceRecord {
    public FiksServiceRecord(FiksAdressing fiksAdressing) {
        super(fiksAdressing.getCertificate().getValue(), ServiceIdentifier.DPF, fiksAdressing.getOrganizationId());
        this.endpointUrl = fiksAdressing.getUrl().toString();
    }

    @Override
    public String getEndPointURL() {
        return endpointUrl;
    }
}

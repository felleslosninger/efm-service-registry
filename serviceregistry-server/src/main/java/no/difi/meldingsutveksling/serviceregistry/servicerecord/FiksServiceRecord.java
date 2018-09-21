package no.difi.meldingsutveksling.serviceregistry.servicerecord;

import lombok.Data;
import no.difi.meldingsutveksling.serviceregistry.model.ServiceIdentifier;

@Data
public class FiksServiceRecord extends ServiceRecord {

    private Integer sikkerhetsnivaa;

    public FiksServiceRecord(String orgnr, Integer sikkerhetsnivaa, String pemCertificate, String endpointUrl) {
        super(pemCertificate, ServiceIdentifier.DPF, orgnr);
        this.endpointUrl = endpointUrl;
        this.sikkerhetsnivaa = sikkerhetsnivaa;
    }

    @Override
    public String getEndPointURL() {
        return endpointUrl;
    }
}

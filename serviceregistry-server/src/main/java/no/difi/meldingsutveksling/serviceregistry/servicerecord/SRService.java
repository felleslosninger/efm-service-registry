package no.difi.meldingsutveksling.serviceregistry.servicerecord;

import lombok.Data;
import lombok.NonNull;
import no.difi.meldingsutveksling.serviceregistry.model.ServiceIdentifier;

@Data
public class SRService {

    @NonNull
    private ServiceIdentifier identifier;
    @NonNull
    protected String endpointUrl;
    private String serviceCode;
    private String serviceEditionCode;
}

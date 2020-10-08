package no.difi.meldingsutveksling.serviceregistry.servicerecord;

import lombok.Data;
import lombok.NonNull;
import no.difi.meldingsutveksling.serviceregistry.domain.ServiceIdentifier;

@Data
public class SRService {

    @NonNull
    private ServiceIdentifier identifier;
    @NonNull
    private String endpointUrl;
    private String serviceCode;
    private String serviceEditionCode;
    private Integer securityLevel;
}

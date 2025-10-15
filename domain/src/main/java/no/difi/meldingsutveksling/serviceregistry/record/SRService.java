package no.difi.meldingsutveksling.serviceregistry.record;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
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

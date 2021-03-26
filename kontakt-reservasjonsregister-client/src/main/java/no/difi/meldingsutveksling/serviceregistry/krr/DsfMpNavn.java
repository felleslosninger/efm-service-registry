package no.difi.meldingsutveksling.serviceregistry.krr;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DsfMpNavn {
    private String forkortetNavn;
}

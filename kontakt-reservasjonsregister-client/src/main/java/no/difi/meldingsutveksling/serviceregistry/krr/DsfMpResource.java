package no.difi.meldingsutveksling.serviceregistry.krr;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DsfMpResource {

    @JsonProperty(value = "personIdentifikator", required = true)
    private String personIdentifier;

    @JsonProperty(value = "navn", required = true)
    private DsfMpNavn navn;

    @JsonProperty(value = "postadresse", required = true)
    private DsfMpPostadresse postadresse;
}

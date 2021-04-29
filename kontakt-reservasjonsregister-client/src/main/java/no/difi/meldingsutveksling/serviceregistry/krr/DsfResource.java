package no.difi.meldingsutveksling.serviceregistry.krr;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DsfResource {

    @JsonProperty(value = "personidentifikator", required = true)
    private String personIdentifier;

    @JsonProperty(value = "navn", required = true)
    private String name;

    @JsonProperty(value = "gateadresse", required = true)
    private String street;

    @JsonProperty(value = "postadresse", required = true)
    private String postAddress;

    @JsonProperty(value = "land", required = true)
    private String country;
}

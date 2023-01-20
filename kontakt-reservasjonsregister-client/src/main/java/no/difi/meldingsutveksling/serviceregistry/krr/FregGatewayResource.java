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
public class FregGatewayResource {
//
//    @JsonProperty(value = "foedselsEllerDNummer", required = true)
//    private String personIdentifier;

    @JsonProperty(value = "navn", required = true)
    private String name;

    @JsonProperty(value = "postadresse", required = true)
    private String postAddress;

    @JsonProperty(value = "gateadresse", required = true)
    private String street;

    @JsonProperty(value = "personIdentifikator", required = true)
    private String personIdentifikator;

    @JsonProperty(value = "foedselsdato", required = true)
    private String foedselsdato;

    @JsonProperty(value = "falskIdentitet", required = true)
    private String falskIdentitet;

    @JsonProperty(value = "land", required = true)
    private String country;
}

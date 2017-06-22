package no.difi.meldingsutveksling.serviceregistry.krr;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
@AllArgsConstructor(staticName = "of")
@NoArgsConstructor
public class ContactInfoResource {

    @JsonProperty(value = "epostadresse")
    private String email;
    @JsonProperty(value = "epostadresse_oppdatert")
    private String emailUpdated;
    @JsonProperty(value = "mobiltelefonnummer")
    private String mobile;
    @JsonProperty(value = "mobiltelefonnummer_oppdatert")
    private String mobileUpdated;
}

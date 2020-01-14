package no.difi.meldingsutveksling.serviceregistry.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A POJO representing the JSON object returned from BRREG
 */
@JsonIgnoreProperties
@NoArgsConstructor
@AllArgsConstructor
@Data
public class BrregEnhet {

    private String organisasjonsnummer;
    private String navn;
    private String organisasjonsform;
    private BrregPostadresse postadresse;
    private BrregPostadresse forretningsadresse;

}

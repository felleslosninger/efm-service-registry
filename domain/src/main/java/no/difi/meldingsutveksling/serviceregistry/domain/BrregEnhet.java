package no.difi.meldingsutveksling.serviceregistry.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

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
    private BrregOrganisasjonsform organisasjonsform;
    private BrregPostadresse postadresse;
    private BrregPostadresse forretningsadresse;
    private LocalDate slettedato;

}

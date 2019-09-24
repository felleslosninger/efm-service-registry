package no.difi.meldingsutveksling.serviceregistry.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import javax.persistence.*;
import javax.persistence.Entity;

/**
 * A POJO representing the JSON object returned from BRREG
 */
@Entity
@JsonIgnoreProperties
@Data
public class BrregEnhet {

    @Id
    private String organisasjonsnummer;
    private String navn;
    private String organisasjonsform;
    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private BrregPostadresse postadresse;
    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private BrregPostadresse forretningsadresse;

}

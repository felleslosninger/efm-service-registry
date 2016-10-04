package no.difi.meldingsutveksling.serviceregistry.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.base.MoreObjects;

import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * A POJO representing the JSON object returned from BRREG
 */
@Entity
@JsonIgnoreProperties
public class BrregEnhet {
    @Id
    String organisasjonsnummer;
    String navn;
    String organisasjonsform;


    public String getOrganisasjonsnummer() {
        return organisasjonsnummer;
    }

    public void setOrganisasjonsnummer(String organisasjonsnummer) {
        this.organisasjonsnummer = organisasjonsnummer;
    }

    public String getNavn() {
        return navn;
    }

    public void setNavn(String navn) {
        this.navn = navn;
    }

    public String getOrganisasjonsform() {
        return organisasjonsform;
    }

    public void setOrganisasjonsform(String organisasjonsform) {
        this.organisasjonsform = organisasjonsform;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("organisasjonsnummer", organisasjonsnummer)
                .add("navn", navn)
                .add("organisasjonsform", organisasjonsform)
                .toString();
    }
}

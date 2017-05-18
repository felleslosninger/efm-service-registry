package no.difi.meldingsutveksling.serviceregistry.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Data
@JsonIgnoreProperties
@JsonInclude(JsonInclude.Include.NON_NULL)
@Entity
public class BrregPostadresse {

    @Id
    @GeneratedValue
    private Integer pId;
    private String adresse;
    private String postnummer;
    private String poststed;
    private String land;

    @JsonIgnore
    public Integer getpId() {
        return this.pId;
    }

    BrregPostadresse() {
    }

    public BrregPostadresse(String adresse, String postnummer, String poststed, String land) {
        this.adresse = adresse;
        this.postnummer = postnummer;
        this.poststed = poststed;
        this.land = land;
    }
}

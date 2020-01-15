package no.difi.meldingsutveksling.serviceregistry.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Postadresse {

    private String adresse;
    private String postnummer;
    private String poststed;
    private String land;

    public static Postadresse of(BrregPostadresse brregPostadresse) {
        if (brregPostadresse == null) {
            return null;
        }
        return new Postadresse()
                .setAdresse(brregPostadresse.getAdresse() == null ? null : String.join(" ", brregPostadresse.getAdresse()))
                .setLand(brregPostadresse.getLand())
                .setPostnummer(brregPostadresse.getPostnummer())
                .setPoststed(brregPostadresse.getPoststed());
    }
}

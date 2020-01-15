package no.difi.meldingsutveksling.serviceregistry.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BrregPostadresse {

    private List<String> adresse;
    private String postnummer;
    private String poststed;
    private String land;

    public String getGateAdresse() {
        String gateAdresse = "";
        if (adresse != null) {
            gateAdresse = String.join(" ", adresse);
            return gateAdresse;
        }
        else
            return null;
        // gjer noko anna her
    }
}

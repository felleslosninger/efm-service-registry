package no.difi.meldingsutveksling.serviceregistry.model.datahotell;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties
public class DatahotellEntry {

    String orgnr;
    String navn;
    String organisasjonsform;
    String postadresse;
    String ppostnr;
    String ppoststed;
    String ppostland;
    String forretningsadr;
    String forradrpostnr;
    String forradrpoststed;
    String forradrland;
}

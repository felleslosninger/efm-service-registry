package no.difi.meldingsutveksling.serviceregistry.krr;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DsfMpPostadresse {
    private List<String> adresselinje;
    private String postnummer;
    private String poststed;
    private String landkode;
}

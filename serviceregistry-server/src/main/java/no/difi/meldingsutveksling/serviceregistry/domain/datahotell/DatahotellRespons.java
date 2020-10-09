package no.difi.meldingsutveksling.serviceregistry.domain.datahotell;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import no.difi.meldingsutveksling.serviceregistry.domain.DatahotellEntry;

import java.util.List;

@Data
@JsonIgnoreProperties
public class DatahotellRespons {

    List<DatahotellEntry> entries;
}

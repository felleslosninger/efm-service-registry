package no.difi.meldingsutveksling.serviceregistry.model.datahotell;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties
public class DatahotellRespons {

    List<DatahotellEntry> entries;
}

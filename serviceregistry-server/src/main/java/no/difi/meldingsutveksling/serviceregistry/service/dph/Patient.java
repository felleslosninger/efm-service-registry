package no.difi.meldingsutveksling.serviceregistry.service.dph;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
public class Patient {
    final private String fnr;
    final private String firstName;
    final private String middleName;
    final private String lastName;
}

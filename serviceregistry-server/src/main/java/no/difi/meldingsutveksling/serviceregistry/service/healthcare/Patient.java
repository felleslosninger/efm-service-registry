package no.difi.meldingsutveksling.serviceregistry.service.healthcare;

import lombok.Data;

@Data
public class Patient {
    final private String fnr;
    final private String firstName;
    final private String middleName;
    final private String lastName;
}

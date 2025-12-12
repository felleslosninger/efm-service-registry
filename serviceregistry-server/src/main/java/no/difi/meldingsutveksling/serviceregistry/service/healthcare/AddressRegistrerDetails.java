package no.difi.meldingsutveksling.serviceregistry.service.healthcare;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AddressRegistrerDetails {
    private String herid1;
    private String herid2;
    private String pemDigdirSertifikat;
    private String ediAdress;
    private String orgNumber;
}

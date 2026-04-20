package no.difi.meldingsutveksling.serviceregistry.service.healthcare;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AddressRegistrerDetails {
    private String herid1;
    private String herid2;
    private String derDigdirSertifikat;
    private String ediAdress;
    private String orgNumber;

    public String getPemCertificate() {
        return "-----BEGIN CERTIFICATE-----\n" + derDigdirSertifikat + "\n-----END CERTIFICATE-----\n";
    }
}


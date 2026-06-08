package no.difi.meldingsutveksling.serviceregistry.service.healthcare;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HealthAddressRegistryDetails {
    private Integer parentHerId;
    private String parentName;
    private Integer herId;
    private String name;
    private String derCertificate;
    private String orgNumber;

    public String getPemCertificate() {
        return "-----BEGIN CERTIFICATE-----\n" + derCertificate + "\n-----END CERTIFICATE-----\n";
    }
}


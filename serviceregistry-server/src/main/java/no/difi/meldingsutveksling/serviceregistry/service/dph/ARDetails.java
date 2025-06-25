package no.difi.meldingsutveksling.serviceregistry.service.dph;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ARDetails{
    private String herIdLevel1;
    private String getHerIdLevel2;
    private String pemCertificate;
    private String ediAdresse;
}

package no.difi.meldingsutveksling.serviceregistry.service.krr;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PrintResponse {
    private String postkasseleverandoerAdresse;
    private String x509Sertifikat;
}

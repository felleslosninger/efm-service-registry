package no.difi.meldingsutveksling.serviceregistry.servicerecord;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import no.difi.meldingsutveksling.serviceregistry.model.ServiceIdentifier;

import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public abstract class ServiceRecord {

    protected String organisationNumber;
    private String pemCertificate;
    private String process;
    private List<String> documentTypes;
    private SRService service;


    public ServiceRecord(ServiceIdentifier serviceIdentifier, String organisationNumber, String endpointUrl) {
        this.organisationNumber = organisationNumber;
        this.service = new SRService(serviceIdentifier, endpointUrl);
    }

    public String getPemCertificate() {
        if (isNullOrEmpty(this.pemCertificate) || this.pemCertificate.contains("BEGIN CERTIFICATE")) {
            return pemCertificate;
        }

        String begin = "-----BEGIN CERTIFICATE-----\n";
        String end = "\n-----END CERTIFICATE-----\n";
        return begin+pemCertificate+end;
    }

}

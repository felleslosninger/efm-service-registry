package no.difi.meldingsutveksling.serviceregistry.record;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import no.difi.meldingsutveksling.serviceregistry.domain.DocumentType;
import no.difi.meldingsutveksling.serviceregistry.domain.Process;
import no.difi.meldingsutveksling.serviceregistry.domain.ServiceIdentifier;

import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.base.Strings.isNullOrEmpty;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class ServiceRecord {

    protected String organisationNumber;
    private String pemCertificate;
    private String process;
    private List<String> documentTypes;
    private SRService service;

    public ServiceRecord(ServiceIdentifier serviceIdentifier,
                         String organisationNumber,
                         Process process,
                         String endpointUrl) {
        this.organisationNumber = organisationNumber;
        this.process = process.getIdentifier();
        this.documentTypes = process.getDocumentTypes().stream().map(DocumentType::getIdentifier).collect(Collectors.toList());
        this.service = new SRService(serviceIdentifier, endpointUrl);
    }

    public String getPemCertificate() {
        if (isNullOrEmpty(this.pemCertificate) || this.pemCertificate.contains("BEGIN CERTIFICATE")) {
            return pemCertificate;
        }

        String begin = "-----BEGIN CERTIFICATE-----\n";
        String end = "\n-----END CERTIFICATE-----\n";
        return begin + pemCertificate + end;
    }

}

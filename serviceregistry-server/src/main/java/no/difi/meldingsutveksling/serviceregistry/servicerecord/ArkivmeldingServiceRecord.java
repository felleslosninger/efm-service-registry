package no.difi.meldingsutveksling.serviceregistry.servicerecord;


import lombok.Data;
import no.difi.meldingsutveksling.serviceregistry.model.ServiceIdentifier;

/**
 * Represents a Service Record for EDU messages
 *
 */
@Data
public class ArkivmeldingServiceRecord extends ServiceRecord {

    private ArkivmeldingServiceRecord(ServiceIdentifier serviceIdentifier, String orgnr, String endpointUrl) {
        super(serviceIdentifier, orgnr, endpointUrl);
    }

    public static ArkivmeldingServiceRecord of(ServiceIdentifier serviceIdentifier, String orgnr, String endpointUrl) {
        return new ArkivmeldingServiceRecord(serviceIdentifier, orgnr, endpointUrl);
    }

    public static ArkivmeldingServiceRecord of(ServiceIdentifier serviceIdentifier, String orgnr, String endpointUrl, String pemCertificate) {
        ArkivmeldingServiceRecord arkivmeldingServiceRecord = new ArkivmeldingServiceRecord(serviceIdentifier, orgnr, endpointUrl);
        arkivmeldingServiceRecord.setPemCertificate(pemCertificate);
        return arkivmeldingServiceRecord;
    }

}

package no.difi.meldingsutveksling.serviceregistry.record;


import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import no.difi.meldingsutveksling.serviceregistry.domain.ServiceIdentifier;

/**
 * Represents a Service Record for EDU messages
 */
@Getter
@Setter
@ToString
@EqualsAndHashCode(callSuper = true)
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

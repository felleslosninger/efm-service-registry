package no.difi.meldingsutveksling.serviceregistry.servicerecord;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import no.difi.meldingsutveksling.serviceregistry.domain.ServiceIdentifier;

@Getter
@Setter
@ToString
@EqualsAndHashCode(callSuper = true)
public class AvtaltServiceRecord extends ServiceRecord {

    private AvtaltServiceRecord(ServiceIdentifier serviceIdentifier, String orgnr, String endpointUrl) {
        super(serviceIdentifier, orgnr, endpointUrl);
    }
    public static AvtaltServiceRecord of(ServiceIdentifier serviceIdentifier, String orgnr, String endpointUrl) {
        return new AvtaltServiceRecord(serviceIdentifier, orgnr, endpointUrl);
    }

    public static AvtaltServiceRecord of(ServiceIdentifier serviceIdentifier, String orgnr, String endpointUrl, String pemCertificate) {
        AvtaltServiceRecord avtaltServiceRecord = new AvtaltServiceRecord(serviceIdentifier, orgnr, endpointUrl);
        avtaltServiceRecord.setPemCertificate(pemCertificate);
        return avtaltServiceRecord;
    }
}


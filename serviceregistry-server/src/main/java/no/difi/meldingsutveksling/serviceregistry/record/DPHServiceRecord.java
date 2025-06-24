package no.difi.meldingsutveksling.serviceregistry.record;


import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import no.difi.meldingsutveksling.serviceregistry.domain.Process;
import no.difi.meldingsutveksling.serviceregistry.domain.ServiceIdentifier;

@Getter
@Setter
@ToString
@EqualsAndHashCode(callSuper = true)
public class DPHServiceRecord extends ServiceRecord {

    private String herIdLevel1;
    private String herIdLevel2;

    public DPHServiceRecord(ServiceIdentifier serviceIdentifier, String organisationNumber, Process process, String endpointUrl) {
        super(serviceIdentifier, organisationNumber, process, endpointUrl);
    }
}

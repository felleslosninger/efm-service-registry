package no.difi.meldingsutveksling.serviceregistry.record;


import lombok.*;
import no.difi.meldingsutveksling.serviceregistry.domain.Process;
import no.difi.meldingsutveksling.serviceregistry.domain.ServiceIdentifier;

@Getter
@Setter
@ToString
@EqualsAndHashCode(callSuper = true)
public class DPHServiceRecord extends ServiceRecord {

    private String herIdLevel1;
    private String herIdLevel2;

    public DPHServiceRecord(ServiceIdentifier serviceIdentifier, String organisationNumber, Process process, String endpointUrl, String herIdLevel1, String herIdLevel2) {
        super(serviceIdentifier, organisationNumber, process, endpointUrl);
        this.herIdLevel1 = herIdLevel1;
        this.herIdLevel2 = herIdLevel2;
    }

}

package no.difi.meldingsutveksling.serviceregistry.record;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import no.difi.meldingsutveksling.serviceregistry.domain.Process;
import no.difi.meldingsutveksling.serviceregistry.domain.ServiceIdentifier;
import no.difi.meldingsutveksling.serviceregistry.service.healthcare.Patient;

@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class HealthCareServiceRecord extends ServiceRecord {

    private String herIdLevel1;
    private String herIdLevel2;
    private Patient patient;

    public HealthCareServiceRecord(){
        super();
    }

    public HealthCareServiceRecord(ServiceIdentifier serviceIdentifier, String organisationNumber, Process process, String endpointUrl, String herIdLevel1, String herIdLevel2, Patient patient) {
        super(serviceIdentifier, organisationNumber, process, endpointUrl);
        this.herIdLevel1 = herIdLevel1;
        this.herIdLevel2 = herIdLevel2;
        this.patient = patient;
    }

}

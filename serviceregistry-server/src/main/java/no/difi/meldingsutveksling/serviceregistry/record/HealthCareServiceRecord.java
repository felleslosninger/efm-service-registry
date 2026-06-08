package no.difi.meldingsutveksling.serviceregistry.record;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import no.difi.meldingsutveksling.serviceregistry.domain.Process;
import no.difi.meldingsutveksling.serviceregistry.domain.ServiceIdentifier;
import no.difi.meldingsutveksling.serviceregistry.service.healthcare.Patient;

@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class HealthCareServiceRecord extends ServiceRecord {

    private Integer herId;
    private Patient patient;

    public HealthCareServiceRecord() {
        super();
    }

    public HealthCareServiceRecord(ServiceIdentifier serviceIdentifier, String organisationNumber, Process process, String endpointUrl, Integer herId, Patient patient) {
        super(serviceIdentifier, organisationNumber, process, endpointUrl);
        this.herId = herId;
        this.patient = patient;
    }

}

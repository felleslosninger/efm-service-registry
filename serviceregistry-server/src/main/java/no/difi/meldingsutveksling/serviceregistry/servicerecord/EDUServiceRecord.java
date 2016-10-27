package no.difi.meldingsutveksling.serviceregistry.servicerecord;

import no.difi.meldingsutveksling.serviceregistry.config.ServiceregistryProperties;

import static no.difi.meldingsutveksling.serviceregistry.model.ServiceIdentifier.EDU;

/**
 * Represents a Service Record for EDU messages
 *
 */
public class EDUServiceRecord extends ServiceRecord {

    EDUServiceRecord(ServiceregistryProperties properties, String pemCertificate, String endpoint, String orgnr) {
        super(properties, pemCertificate, EDU, orgnr);
        this.endpointUrl = endpoint;
    }

    @Override
    public String getEndPointURL() {
        return endpointUrl;
    }

    @Override
    public String getOrganisationNumber() {
        return organisationNumber;
    }
}

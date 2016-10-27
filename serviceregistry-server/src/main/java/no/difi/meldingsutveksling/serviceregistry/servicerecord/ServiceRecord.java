package no.difi.meldingsutveksling.serviceregistry.servicerecord;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import no.difi.meldingsutveksling.serviceregistry.model.ServiceIdentifier;

import java.io.Serializable;
import no.difi.meldingsutveksling.serviceregistry.config.ServiceregistryProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class ServiceRecord implements Serializable {

    protected ServiceregistryProperties properties;
    protected String organisationNumber;
    private ServiceIdentifier serviceIdentifier;
    protected String endpointUrl;
    protected String pemCertificate;

    public ServiceRecord(ServiceregistryProperties e, String pemCertificate, ServiceIdentifier serviceIdentifier, String organisationNumber) {
        this.organisationNumber = organisationNumber;
        this.serviceIdentifier = serviceIdentifier;
        this.pemCertificate = pemCertificate;
        this.properties = e;
    }

    public String getOrganisationNumber() {
        return organisationNumber;
    }

    public void setOrganisationNumber(String organisationNumber) {
        this.organisationNumber = organisationNumber;
    }

    public abstract String getEndPointURL();

    public ServiceIdentifier getServiceIdentifier() {
        return serviceIdentifier;
    }

    public void setServiceIdentifier(ServiceIdentifier serviceIdentifier) {
        this.serviceIdentifier = serviceIdentifier;
    }

    public String getPemCertificate() {
        return pemCertificate;
    }

    @Override
    public String toString() {
        return "ServiceRecord{"
                + "serviceIdentifier='" + serviceIdentifier + '\''
                + ", organisationNumber='" + organisationNumber + '\''
                + ", pemCertificate='" + getPemCertificate() + '\''
                + ", endPointURL='" + getEndPointURL() + '\''
                + '}';
    }
}

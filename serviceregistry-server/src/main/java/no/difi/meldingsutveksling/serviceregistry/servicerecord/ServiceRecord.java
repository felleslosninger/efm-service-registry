package no.difi.meldingsutveksling.serviceregistry.servicerecord;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.collect.Lists;
import no.difi.meldingsutveksling.serviceregistry.model.ServiceIdentifier;

import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;

@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class ServiceRecord {

    protected String organisationNumber;
    private ServiceIdentifier serviceIdentifier;
    protected String endpointUrl;
    private String pemCertificate;
    private List<String> dpeCapabilities;

    public ServiceRecord(String pemCertificate, ServiceIdentifier serviceIdentifier, String organisationNumber) {
        this.organisationNumber = organisationNumber;
        this.serviceIdentifier = serviceIdentifier;
        this.pemCertificate = pemCertificate;
        this.dpeCapabilities = Lists.newArrayList();
    }

    public String getOrganisationNumber() {
        return organisationNumber;
    }

    public void setOrganisationNumber(String organisationNumber) {
        this.organisationNumber = organisationNumber;
    }

    public abstract String getEndPointURL();

    public void setEndpointUrl(String endpointUrl) {
        this.endpointUrl = endpointUrl;
    }

    public ServiceIdentifier getServiceIdentifier() {
        return serviceIdentifier;
    }

    public void setServiceIdentifier(ServiceIdentifier serviceIdentifier) {
        this.serviceIdentifier = serviceIdentifier;
    }

    public String getPemCertificate() {
        if (isNullOrEmpty(this.pemCertificate) || this.pemCertificate.contains("BEGIN CERTIFICATE")) {
            return pemCertificate;
        }

        String begin = "-----BEGIN CERTIFICATE-----\n";
        String end = "\n-----END CERTIFICATE-----\n";
        return begin+pemCertificate+end;
    }

    public List<String> getDpeCapabilities() {
        return dpeCapabilities;
    }

    public void addDpeCapability(String capability) {
        this.dpeCapabilities.add(capability);
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

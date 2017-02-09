package no.difi.meldingsutveksling.serviceregistry.service.ks;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.base.MoreObjects;

import java.net.URL;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FiksContactInfo {
    static final FiksContactInfo EMPTY = new FiksContactInfo();
    private String organizationId;
    private URL url;
    private PemCertificate certificate;

    public String getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
    }

    public URL getUrl() {
        return url;
    }

    public void setUrl(URL url) {
        this.url = url;
    }

    public PemCertificate getCertificate() {
        return certificate;
    }

    public void setCertificate(PemCertificate certificate) {
        this.certificate = certificate;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("organizationId", organizationId)
                .add("url", url)
                .add("certificate", certificate)
                .toString();
    }
}

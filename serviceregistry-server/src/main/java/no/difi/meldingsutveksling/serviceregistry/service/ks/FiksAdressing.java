package no.difi.meldingsutveksling.serviceregistry.service.ks;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

import java.net.URL;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FiksAdressing {
    static final FiksAdressing EMPTY = new FiksAdressing();
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

    public boolean shouldUseFIKS() {
        return this != EMPTY;
    }

    @Override
    @SuppressWarnings({"squid:S00122", "squid:S1067"})
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FiksAdressing that = (FiksAdressing) o;
        return Objects.equal(organizationId, that.organizationId) &&
                Objects.equal(url, that.url) &&
                Objects.equal(certificate, that.certificate);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(organizationId, url, certificate);
    }
}

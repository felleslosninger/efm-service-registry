package no.difi.meldingsutveksling.serviceregistry.service.virksert;

import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryException;
import no.difi.meldingsutveksling.serviceregistry.config.ServiceregistryProperties;
import no.difi.vefa.peppol.common.model.ParticipantIdentifier;
import no.difi.vefa.peppol.common.model.ProcessIdentifier;
import no.difi.vefa.peppol.common.model.Scheme;
import no.difi.virksert.api.Mode;
import no.difi.virksert.client.BusinessCertificateClient;
import no.difi.virksert.client.lang.VirksertClientException;
import no.difi.virksert.lang.BusinessCertificateException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.cert.X509Certificate;

@Component
public class VirkSertService {

    private BusinessCertificateClient virksertClient;

    @Autowired
    private ServiceregistryProperties properties;

    @Autowired
    private Environment env;

    public VirkSertService() {
    }

    public VirkSertService(BusinessCertificateClient virksertClient) {
        this.virksertClient = virksertClient;
    }

    @PostConstruct
    public void init() {

        URI virksertUrl = null;
        try {
            virksertUrl = properties.getAr().getEndpointURL().toURI();
        } catch (URISyntaxException e) {
            throw new ServiceRegistryException(e);
        }

        try {
            if (env.acceptsProfiles(Profiles.of("production"))) {
                virksertClient = BusinessCertificateClient.of(virksertUrl, Mode.PRODUCTION);
            } else {
                virksertClient = BusinessCertificateClient.of(virksertUrl, "/recipe-move-difiSigned.xml");
            }
        } catch (BusinessCertificateException e) {
            throw new ServiceRegistryException(e);
        }
    }

    public String getCertificate(String orgNumber) throws VirksertClientException {
        X509Certificate cert = virksertClient.fetchCertificate(ParticipantIdentifier.of("9908:"+orgNumber),
                ProcessIdentifier.of(properties.getAr().getProcessIdentifier(), Scheme.of(properties.getAr().getSchema())));
        return CertificateToString.toString(cert);
    }
}

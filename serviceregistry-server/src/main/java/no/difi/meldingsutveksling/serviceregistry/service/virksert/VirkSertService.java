package no.difi.meldingsutveksling.serviceregistry.service.virksert;

import no.difi.meldingsutveksling.serviceregistry.config.ServiceregistryProperties;
import no.difi.vefa.peppol.common.model.ParticipantIdentifier;
import no.difi.vefa.peppol.common.model.ProcessIdentifier;
import no.difi.virksert.api.Mode;
import no.difi.virksert.client.BusinessCertificateClient;
import no.difi.virksert.client.lang.VirksertClientException;
import no.difi.virksert.lang.BusinessCertificateException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
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
    public void init() throws URISyntaxException, BusinessCertificateException {
        if (env.acceptsProfiles("production")) {
            virksertClient = BusinessCertificateClient.of(properties.getAr().getEndpointURL().toURI(), Mode.PRODUCTION);
        } else {
            virksertClient = BusinessCertificateClient.of(properties.getAr().getEndpointURL().toURI(), Mode.TEST);
        }
    }

    public String getCertificate(String orgNumber) throws VirksertClientException {
        X509Certificate cert = virksertClient.fetchCertificate(ParticipantIdentifier.of(orgNumber),
                ProcessIdentifier.of(properties.getAr().getProcessIdentifier()));
        return CertificateToString.toString(cert);
    }
}

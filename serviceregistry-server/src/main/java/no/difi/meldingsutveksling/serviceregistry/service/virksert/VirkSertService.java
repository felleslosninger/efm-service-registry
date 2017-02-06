package no.difi.meldingsutveksling.serviceregistry.service.virksert;

import no.difi.meldingsutveksling.serviceregistry.config.ServiceregistryProperties;
import no.difi.virksert.client.VirksertClient;
import no.difi.virksert.client.VirksertClientBuilder;
import no.difi.virksert.client.VirksertClientException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.security.cert.X509Certificate;

@Component
public class VirkSertService {

    private VirksertClient virksertClient;

    @Autowired
    private ServiceregistryProperties properties;

    public VirkSertService() {
    }

    public VirkSertService(VirksertClient virksertClient) {
        this.virksertClient = virksertClient;
    }

    @PostConstruct
    public void init() {
        virksertClient = VirksertClientBuilder.newInstance()
                .setUri(properties.getAr().getEndpointURL().toString()).build();
    }

    public String getCertificate(String orgNumber) throws VirksertClientException {
        final X509Certificate fetch = virksertClient.fetch(orgNumber);
        return CertificateToString.toString(fetch);
    }
}

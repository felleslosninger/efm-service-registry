package no.difi.meldingsutveksling.serviceregistry.service.virksert;

import java.security.cert.Certificate;
import javax.annotation.PostConstruct;
import no.difi.meldingsutveksling.serviceregistry.config.ServiceregistryProperties;
import no.difi.virksert.client.VirksertClient;
import no.difi.virksert.client.VirksertClientBuilder;
import no.difi.virksert.client.VirksertClientException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
                .setScope("no.difi.meldingsutveksling.serviceregistry.service.virksert.DemoScope")
                .setUri(properties.getAr().getEndpointURL().toString()).build();
    }

    public Certificate getCertificate(String orgNumber) throws VirksertClientException {
        return virksertClient.fetch(orgNumber);
    }
}

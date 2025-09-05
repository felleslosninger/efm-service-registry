package no.difi.meldingsutveksling.serviceregistry.config;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.serviceregistry.exceptions.ServiceRegistryException;
import no.difi.virksert.client.BusinessCertificateClient;
import no.difi.virksert.lang.BusinessCertificateException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URI;
import java.net.URISyntaxException;

@Configuration
@RequiredArgsConstructor
public class VirksertConfig {
    private final ServiceregistryProperties properties;

    @Bean
    public BusinessCertificateClient virksertClient() {
        try {
            URI virksertUrl = properties.getVirksert().getEndpointURL().toURI();
            return BusinessCertificateClient.of(virksertUrl, properties.getVirksert().getMode());
        } catch (URISyntaxException | BusinessCertificateException e) {
            throw new ServiceRegistryException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

package no.difi.meldingsutveksling.serviceregistry.config;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.serviceregistry.exceptions.ServiceRegistryException;
import no.difi.virksert.api.Mode;
import no.difi.virksert.client.BusinessCertificateClient;
import no.difi.virksert.lang.BusinessCertificateException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.net.URI;
import java.net.URISyntaxException;

@Configuration
@RequiredArgsConstructor
public class VirksertConfig {
    private final ServiceregistryProperties properties;

    @Bean
    public BusinessCertificateClient virksertClient() {
        BusinessCertificateClient virksertClient;

        URI virksertUrl;
        try {
            virksertUrl = properties.getVirksert().getEndpointURL().toURI();
        } catch (URISyntaxException e) {
            throw new ServiceRegistryException(e);
        }

        try {
            if (properties.getVirksert().getMode().equalsIgnoreCase("prod")) {
                virksertClient = BusinessCertificateClient.of(virksertUrl, Mode.PRODUCTION);
            } else if (properties.getVirksert().getMode().equalsIgnoreCase("move")) {
                virksertClient = BusinessCertificateClient.of(virksertUrl, Mode.MOVE);

              // Are these needed? Think they are only used by Virksert itself?
            } else if (properties.getVirksert().getMode().equalsIgnoreCase("codecept")) {
                virksertClient = BusinessCertificateClient.of(virksertUrl, Mode.CODECEPTJS);
            } else if (properties.getVirksert().getMode().equalsIgnoreCase("test")) {
                virksertClient = BusinessCertificateClient.of(virksertUrl, Mode.TEST);

              // Is defaulting to MOVE sensible? Easier to set up, but could cause issues if not set/there are typos in prod.
              // Maybe better to be strict when it comes to typos, and throw exception, forcing user to have an active relationship with the property?
            } else {
                virksertClient = BusinessCertificateClient.of(virksertUrl, Mode.MOVE);
            }
        } catch (BusinessCertificateException e) {
            throw new ServiceRegistryException(e);
        }
        return virksertClient;
    }
}

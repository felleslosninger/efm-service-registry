package no.difi.meldingsutveksling.serviceregistry.service.brreg;

import no.difi.meldingsutveksling.serviceregistry.client.brreg.BrregClient;
import no.difi.meldingsutveksling.serviceregistry.client.brreg.BrregMockClient;
import no.difi.meldingsutveksling.serviceregistry.config.ServiceregistryProperties;
import no.difi.meldingsutveksling.serviceregistry.service.brreg.dev.TestEnvironmentEnheter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.net.URISyntaxException;

@Configuration
@Profile({"dev", "test", "staging"})
public class BrregDevConfig {

    @Bean
    BrregClient brregClient(TestEnvironmentEnheter enheter, ServiceregistryProperties properties) throws URISyntaxException {
        return new BrregMockClient(enheter, properties);
    }
}

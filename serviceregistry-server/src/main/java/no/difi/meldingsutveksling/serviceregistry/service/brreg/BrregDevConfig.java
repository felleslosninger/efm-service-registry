package no.difi.meldingsutveksling.serviceregistry.service.brreg;

import no.difi.meldingsutveksling.serviceregistry.client.brreg.BrregClient;
import no.difi.meldingsutveksling.serviceregistry.client.brreg.BrregMockClient;
import no.difi.meldingsutveksling.serviceregistry.config.ServiceregistryProperties;
import no.difi.meldingsutveksling.serviceregistry.service.brreg.dev.TestEnvironmentEnheter;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URISyntaxException;

@Configuration
@ConditionalOnProperty(prefix = "difi.move.brreg", name = "enabled", havingValue = "false")
public class BrregDevConfig {

    @Bean
    BrregClient brregClient(TestEnvironmentEnheter enheter, ServiceregistryProperties properties) throws URISyntaxException {
        return new BrregMockClient(enheter, properties);
    }
}

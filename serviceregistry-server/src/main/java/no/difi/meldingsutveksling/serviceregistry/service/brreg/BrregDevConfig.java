package no.difi.meldingsutveksling.serviceregistry.service.brreg;

import java.net.URISyntaxException;
import no.difi.meldingsutveksling.serviceregistry.config.ServiceregistryProperties;
import no.difi.meldingsutveksling.serviceregistry.client.brreg.BrregClient;
import no.difi.meldingsutveksling.serviceregistry.client.brreg.BrregMockClient;
import no.difi.meldingsutveksling.serviceregistry.service.brreg.dev.TestEnvironmentEnheter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile({"dev", "test", "itest", "systest", "staging"})
public class BrregDevConfig {

    @Autowired
    TestEnvironmentEnheter enheter;

    @Autowired
    ServiceregistryProperties properties;

    @Bean
    BrregClient brregClient() throws URISyntaxException {
        return new BrregMockClient(enheter, properties);
    }
}

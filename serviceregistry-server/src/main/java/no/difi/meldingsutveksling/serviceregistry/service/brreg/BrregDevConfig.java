package no.difi.meldingsutveksling.serviceregistry.service.brreg;

import no.difi.meldingsutveksling.serviceregistry.client.brreg.BrregClient;
import no.difi.meldingsutveksling.serviceregistry.client.brreg.BrregMockClient;
import no.difi.meldingsutveksling.serviceregistry.model.BrregEnhet;
import no.difi.meldingsutveksling.serviceregistry.service.brreg.dev.TestEnvironmentEnheter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

import java.util.Map;
import java.util.Optional;

@Configuration
@Profile({"dev", "test", "itest", "systest"})
public class BrregDevConfig {

    @Autowired
    TestEnvironmentEnheter enheter;

    @Autowired
    Environment environment;

    @Bean
    BrregClient brregClient() {
        return new BrregMockClient(enheter, environment);
    }
}

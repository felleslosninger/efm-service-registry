package no.difi.meldingsutveksling.serviceregistry.service.brreg;

import java.net.URI;
import no.difi.meldingsutveksling.serviceregistry.client.brreg.BrregClient;
import no.difi.meldingsutveksling.serviceregistry.client.brreg.BrregClientImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

@Configuration
@Profile({"production"})
public class BrregProdConfig {

    @Autowired
    Environment environment;

    @Bean
    BrregClient brregClient() {
        return new BrregClientImpl(URI.create(environment.getProperty("brreg.enhetsregister.url")));
    }
}

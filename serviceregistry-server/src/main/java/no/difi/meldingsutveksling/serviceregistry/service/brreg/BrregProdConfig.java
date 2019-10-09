package no.difi.meldingsutveksling.serviceregistry.service.brreg;

import no.difi.meldingsutveksling.serviceregistry.client.brreg.BrregClient;
import no.difi.meldingsutveksling.serviceregistry.client.brreg.BrregClientImpl;
import no.difi.meldingsutveksling.serviceregistry.config.ServiceregistryProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.net.URISyntaxException;

@Configuration
@Profile({"production"})
public class BrregProdConfig {

    @Bean
    BrregClient brregClient(ServiceregistryProperties properties) throws URISyntaxException {
        return new BrregClientImpl(properties.getBrreg().getEndpointURL().toURI());
    }
}

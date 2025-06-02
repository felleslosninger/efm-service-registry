package no.difi.meldingsutveksling.serviceregistry.service.brreg;

import no.difi.meldingsutveksling.serviceregistry.client.brreg.BrregClient;
import no.difi.meldingsutveksling.serviceregistry.client.brreg.BrregClientImpl;
import no.difi.meldingsutveksling.serviceregistry.config.ServiceregistryProperties;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.net.URISyntaxException;

@Configuration
@ConditionalOnProperty(prefix = "difi.move.brreg", name = "enabled", havingValue = "true")
public class BrregProdConfig {

    @Bean
    BrregClient brregClient(ServiceregistryProperties properties) throws URISyntaxException {
        return new BrregClientImpl(properties.getBrreg().getEndpointURL().toURI());
    }
}

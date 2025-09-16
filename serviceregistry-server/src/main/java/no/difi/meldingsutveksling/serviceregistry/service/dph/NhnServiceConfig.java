package no.difi.meldingsutveksling.serviceregistry.service.dph;

import no.difi.meldingsutveksling.serviceregistry.config.ServiceregistryProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NhnServiceConfig {

    @Bean
    public NhnService nhnService(ServiceregistryProperties serviceregistryProperties) {
        return new NhnService(serviceregistryProperties.getDph().nhnAdapterEndepunkt());
    }
}

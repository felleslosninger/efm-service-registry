package no.difi.meldingsutveksling.serviceregistry.service.ks;

import no.difi.meldingsutveksling.serviceregistry.config.ServiceregistryProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableConfigurationProperties(ServiceregistryProperties.class)
public class FiksAdresseServiceConfiguration {
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }

    @Bean
    public FiksAdresseClient fiksAdresseClient(RestTemplate restTemplate, ServiceregistryProperties properties) {
        return new FiksAdresseClient(restTemplate, properties.getFiks().getAdresseServiceURL());
    }

}

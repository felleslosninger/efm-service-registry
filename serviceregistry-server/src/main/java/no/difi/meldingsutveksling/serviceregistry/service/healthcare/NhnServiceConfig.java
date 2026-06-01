package no.difi.meldingsutveksling.serviceregistry.service.healthcare;

import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.serviceregistry.config.ServiceregistryProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Slf4j
@Configuration
public class NhnServiceConfig {
    
    @Bean
    public NhnService nhnService(ServiceregistryProperties properties) {
        return new NhnService(properties.getHealthcare().getEndpointURL(), restClient(properties));
    }

    private RestClient restClient(ServiceregistryProperties properties) {
        return RestClient.builder()
                .requestFactory(getClientHttpRequestFactory(properties))
                .build();
    }

    private ClientHttpRequestFactory getClientHttpRequestFactory(ServiceregistryProperties properties) {
        HttpComponentsClientHttpRequestFactory clientHttpRequestFactory = new HttpComponentsClientHttpRequestFactory();
        clientHttpRequestFactory.setConnectionRequestTimeout(properties.getHealthcare().getTimeout().getConnect());
        clientHttpRequestFactory.setReadTimeout(properties.getHealthcare().getTimeout().getRead());
        return clientHttpRequestFactory;
    }
}

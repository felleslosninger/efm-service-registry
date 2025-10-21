package no.difi.meldingsutveksling.serviceregistry.service.dph;

import no.difi.meldingsutveksling.serviceregistry.config.ServiceregistryProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class NhnServiceConfig {

    private final static String NHN_CLIENT_NAME = "NhnRestClient";

    @Bean
    public NhnService nhnService(ServiceregistryProperties serviceregistryProperties,@Qualifier("NHN_CLIENT_NAME") RestClient restClient) {
        return new NhnService(serviceregistryProperties.getDph().nhnAdapterEndpointUrl(),restClient);
    }

    @Bean("NHN_CLIENT_NAME")
    public RestClient restClient() {
        return RestClient.create();
    }
}

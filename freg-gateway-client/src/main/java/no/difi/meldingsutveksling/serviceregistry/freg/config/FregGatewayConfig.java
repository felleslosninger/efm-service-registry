package no.difi.meldingsutveksling.serviceregistry.freg.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
@RequiredArgsConstructor
public class FregGatewayConfig {

    @Bean(name = "fregGatewayRestTemplate")
    public RestTemplate fregGatewayRestTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        return restTemplate;
    }
}
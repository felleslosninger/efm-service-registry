package no.difi.meldingsutveksling.serviceregistry.freg.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

@Configuration
@RequiredArgsConstructor
public class FregGatewayConfig {
    public static final String CLIENT_ID = "Client-Id";

    @Bean(name = "fregGatewayRestTemplate")
    public RestTemplate fregGatewayRestTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        return restTemplate;
    }

    protected ClientHttpRequestInterceptor clientIdRequestInterceptor() {
        return (request, body, execution) -> {
            request.getHeaders().add(CLIENT_ID, "TestClient");
            return execution.execute(request, body);
        };
    }
}
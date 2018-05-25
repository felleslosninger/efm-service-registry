package no.difi.meldingsutveksling.serviceregistry;

import no.difi.meldingsutveksling.serviceregistry.config.ServiceregistryProperties;
import no.difi.move.common.config.SpringCloudProtocolResolver;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;

@SpringBootApplication
@EnableCircuitBreaker
@EnableConfigurationProperties({ServiceregistryProperties.class})
public class MoveServiceRegistryApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(MoveServiceRegistryApplication.class)
                .initializers(new SpringCloudProtocolResolver())
                .run(args);
    }
}

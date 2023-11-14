package no.difi.meldingsutveksling.serviceregistry;

import no.difi.meldingsutveksling.serviceregistry.config.ServiceregistryProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({ServiceregistryProperties.class})
public class MoveServiceRegistryApplication {

    public static void main(String... args) {
        SpringApplication.run(MoveServiceRegistryApplication.class, args);
    }
}

package no.difi.meldingsutveksling.serviceregistry;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories
public class MoveServiceRegistryApplication {
    public static void main(String[] args) {
        SpringApplication.run(MoveServiceRegistryApplication.class, args);
    }
}
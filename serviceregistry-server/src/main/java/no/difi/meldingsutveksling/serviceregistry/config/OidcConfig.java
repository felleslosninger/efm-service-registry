package no.difi.meldingsutveksling.serviceregistry.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "difi.move.auth.enable", havingValue = "true")
public class OidcConfig {
}

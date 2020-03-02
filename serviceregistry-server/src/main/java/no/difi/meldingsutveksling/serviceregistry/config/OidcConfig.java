package no.difi.meldingsutveksling.serviceregistry.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;

@Configuration
@ConditionalOnProperty(name = "difi.move.auth.enable", havingValue = "true")
@EnableResourceServer
public class OidcConfig {
}

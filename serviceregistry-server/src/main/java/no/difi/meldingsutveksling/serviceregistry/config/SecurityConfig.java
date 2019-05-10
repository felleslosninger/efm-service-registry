package no.difi.meldingsutveksling.serviceregistry.config;

import no.difi.meldingsutveksling.serviceregistry.auth.TokenAuthenticationFilter;
import no.difi.meldingsutveksling.serviceregistry.auth.TokenValidator;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true)
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    private final TokenValidator tokenValidator;

    public SecurityConfig(TokenValidator tokenValidator) {
        this.tokenValidator = tokenValidator;
    }

    @Override
    protected void configure(final HttpSecurity http) {
        final TokenAuthenticationFilter tokenFilter = new TokenAuthenticationFilter(tokenValidator);
        http.addFilterBefore(tokenFilter, BasicAuthenticationFilter.class);
    }
}
package no.difi.meldingsutveksling.serviceregistry.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtIssuerAuthenticationManagerResolver;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
@SuppressWarnings("squid:S1118")
public class WebSecurityConfig {

    @Configuration
    @RequiredArgsConstructor
    @Order(0)
    public static class SecurityFilter extends WebSecurityConfigurerAdapter {

        private final ServiceregistryProperties props;
        private final SecurityProperties securityProperties;

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            var jwtIssuerAuthenticationManagerResolver =
                    new JwtIssuerAuthenticationManagerResolver(props.getAuth().getMaskinportenIssuer());

            http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                    .and().csrf().disable();
            http.authorizeRequests()
                    .antMatchers("/health/**", "/prometheus", "/h2-console/**", "/jwk").permitAll()
                    .and()
                    .headers().frameOptions().sameOrigin().and()
                    .authorizeRequests()
                    .antMatchers("/api/**").authenticated()
                    .and()
                    .httpBasic()
                    .and()
                    .authorizeRequests()
                    .anyRequest().authenticated()
                    .and().oauth2ResourceServer(o -> o.authenticationManagerResolver(jwtIssuerAuthenticationManagerResolver));
        }

        @Override
        protected void configure(AuthenticationManagerBuilder auth) throws Exception {
            auth.inMemoryAuthentication().withUser(securityProperties.getUser().getName())
                    .password("{noop}" + securityProperties.getUser().getPassword()).roles();
        }
    }
}

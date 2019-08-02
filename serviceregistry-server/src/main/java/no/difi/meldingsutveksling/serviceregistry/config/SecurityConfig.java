package no.difi.meldingsutveksling.serviceregistry.config;

import no.difi.meldingsutveksling.serviceregistry.auth.OidcRemoteTokenServices;
import no.difi.meldingsutveksling.serviceregistry.auth.OrgnrUserAuthConverter;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.provider.token.DefaultAccessTokenConverter;

@Configuration
@EnableResourceServer
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Bean
    @ConditionalOnProperty(name = "difi.move.auth.enable", havingValue = "true")
    public BeanPostProcessor configureRemoteTokenService() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessBeforeInitialization(Object o, String string) throws BeansException {
                return o;
            }

            @Override
            public Object postProcessAfterInitialization(Object o, String string) throws BeansException {
                if (o instanceof OidcRemoteTokenServices) {
                    OidcRemoteTokenServices tokenServices = (OidcRemoteTokenServices) o;
                    OrgnrUserAuthConverter orgnrUserAuthConverter = new OrgnrUserAuthConverter();
                    DefaultAccessTokenConverter defaultAccessTokenConverter = new DefaultAccessTokenConverter();
                    defaultAccessTokenConverter.setUserTokenConverter(orgnrUserAuthConverter);
                    tokenServices.setAccessTokenConverter(defaultAccessTokenConverter);
                }
                return o;
            }
        };
    }

    @Configuration
    public static class AdminApiSecurityConfiguration extends WebSecurityConfigurerAdapter {

        protected void configure(HttpSecurity http) throws Exception {
            http.headers().frameOptions().sameOrigin();
            http.csrf().disable()
                    .authorizeRequests()
                    .antMatchers("/h2-console/**").permitAll()
                    .antMatchers("/api/**").authenticated()
                    .and().httpBasic();
        }

    }

}

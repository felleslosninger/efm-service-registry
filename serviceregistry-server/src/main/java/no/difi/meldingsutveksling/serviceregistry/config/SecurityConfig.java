package no.difi.meldingsutveksling.serviceregistry.config;

import no.difi.meldingsutveksling.serviceregistry.auth.OidcRemoteTokenServices;
import no.difi.meldingsutveksling.serviceregistry.auth.OrgnrUserAuthConverter;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.provider.token.DefaultAccessTokenConverter;

@ConditionalOnProperty(name = "difi.move.auth.enable", havingValue = "true")
@Configuration
@EnableResourceServer
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Bean
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

}

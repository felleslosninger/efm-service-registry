package no.difi.meldingsutveksling.serviceregistry.config;

import no.difi.meldingsutveksling.serviceregistry.auth.OrgnrUserAuthConverter;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.provider.token.DefaultAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.RemoteTokenServices;

@Configuration
@EnableResourceServer
public class SecurityConfig {

    @Bean
    @ConditionalOnProperty(name = "difi.move.auth.enable", havingValue = "true")
    public BeanPostProcessor configureRemoteTokenService() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessBeforeInitialization(Object o, String string) {
                return o;
            }

            @Override
            public Object postProcessAfterInitialization(Object o, String string) {
                if (o instanceof RemoteTokenServices) {
                    RemoteTokenServices tokenServices = (RemoteTokenServices) o;
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

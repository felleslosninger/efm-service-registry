package no.difi.meldingsutveksling.serviceregistry.config;

import no.difi.move.common.config.KeystoreProperties;
import no.difi.move.common.oauth.KeystoreHelper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SRConfig implements WebMvcConfigurer {

    @Bean
    HystrixContextInterceptor hystrixContextInterceptor() {
        return new HystrixContextInterceptor();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(hystrixContextInterceptor());
    }

    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        configurer.defaultContentType(MediaType.APPLICATION_JSON);
    }

    @Bean
    public KeystoreHelper keystoreHelper(ServiceregistryProperties srProps) {
        KeystoreProperties props = new KeystoreProperties();
        props.setAlias(srProps.getSign().getJks().getAlias());
        props.setEntryPassword(srProps.getSign().getJks().getPassword());
        props.setStorePassword(srProps.getSign().getJks().getPassword());
        props.setLocation(srProps.getSign().getJks().getKeystore());

        return new KeystoreHelper(props);
    }
}

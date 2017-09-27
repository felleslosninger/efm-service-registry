package no.difi.meldingsutveksling.serviceregistry.config;

import no.difi.move.common.config.KeystoreProperties;
import no.difi.move.common.oauth.KeystoreHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

@Configuration
public class SRConfig extends WebMvcConfigurerAdapter {

    @Autowired
    private ServiceregistryProperties srProps;

    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        configurer.defaultContentType(MediaType.APPLICATION_JSON);
    }

    @Bean
    KeystoreHelper keystoreHelper() {
        KeystoreProperties props = new KeystoreProperties();
        props.setAlias(srProps.getSign().getJks().getAlias());
        props.setEntryPassword(srProps.getSign().getJks().getPassword());
        props.setStorePassword(srProps.getSign().getJks().getPassword());
        props.setLocation(srProps.getSign().getJks().getKeystore());

        return new KeystoreHelper(props);
    }
}

package no.difi.meldingsutveksling.serviceregistry.config;

import no.difi.move.common.config.KeystoreProperties;
import no.difi.move.common.oauth.KeystoreHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SRConfig {

    @Autowired
    private ServiceregistryProperties srProps;

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

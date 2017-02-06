package no.difi.meldingsutveksling.serviceregistry.config;

import no.difi.move.common.config.KeystoreProperties;
import no.difi.move.common.oauth.KeystoreHelper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

@Configuration
public class SRConfig {

    @Bean
    KeystoreHelper keystoreHelper(
            @Value("${difi.move.sign.jks.alias}") String alias,
            @Value("${difi.move.sign.jks.password}") String password,
            @Value("${difi.move.sign.jks.keystore}") Resource keystore) {
        KeystoreProperties props = new KeystoreProperties();
        props.setAlias(alias);
        props.setEntryPassword(password);
        props.setStorePassword(password);
        props.setLocation(keystore);

        return new KeystoreHelper(props);
    }
}

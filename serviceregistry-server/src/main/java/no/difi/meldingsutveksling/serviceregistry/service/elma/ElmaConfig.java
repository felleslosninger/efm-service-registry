package no.difi.meldingsutveksling.serviceregistry.service.elma;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.serviceregistry.config.ServiceregistryProperties;
import no.difi.vefa.peppol.common.lang.PeppolLoadingException;
import no.difi.vefa.peppol.lookup.LookupClient;
import no.difi.vefa.peppol.lookup.LookupClientBuilder;
import no.difi.vefa.peppol.lookup.locator.StaticLocator;
import no.difi.vefa.peppol.security.util.EmptyCertificateValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class ElmaConfig {

    private final ServiceregistryProperties props;

    @Bean
    public LookupClient getElmaLookupClient() throws PeppolLoadingException {
        return LookupClientBuilder.forTest()
                .locator(new StaticLocator(props.getElma().getLocatorUrl()))
                .certificateValidator(EmptyCertificateValidator.INSTANCE)
                .build();
    }
}
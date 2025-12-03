package no.difi.meldingsutveksling.serviceregistry.service.elma;

import lombok.RequiredArgsConstructor;
import network.oxalis.vefa.peppol.common.lang.PeppolLoadingException;
import network.oxalis.vefa.peppol.lookup.LookupClient;
import network.oxalis.vefa.peppol.lookup.LookupClientBuilder;
import network.oxalis.vefa.peppol.lookup.locator.StaticLocator;
import network.oxalis.vefa.peppol.security.util.EmptyCertificateValidator;
import no.difi.meldingsutveksling.serviceregistry.config.ServiceregistryProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class ElmaConfig {

    private final ServiceregistryProperties props;

    @Bean
    @Qualifier("defaultLookupClient")
    public LookupClient getElmaLookupClient() throws PeppolLoadingException {
        return LookupClientBuilder.forTest()
                .locator(new StaticLocator(props.getElma().getLocatorUrl()))
                .certificateValidator(EmptyCertificateValidator.INSTANCE)
                .build();
    }

    @Bean
    public EformidlingLookupClientWrapper getEformidlingLookupClient() throws PeppolLoadingException {
        LookupClient wrappedClient = LookupClientBuilder.forTest()
                .locator(new StaticLocator(props.getElma().getLocatorUrl()))
                .provider(CustomServiceMetadataProvider.class)
                .certificateValidator(EmptyCertificateValidator.INSTANCE)
                .build();
        return new EformidlingLookupClientWrapper(wrappedClient);
    }
}
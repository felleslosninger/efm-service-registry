package no.difi.meldingsutveksling.serviceregistry.service.elma;

import no.difi.vefa.peppol.common.model.TransportProfile;
import no.difi.vefa.peppol.lookup.LookupClient;
import no.difi.vefa.peppol.lookup.LookupClientBuilder;
import no.difi.vefa.peppol.lookup.locator.BusdoxLocator;
import no.difi.vefa.peppol.lookup.locator.DynamicLocator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * @author Glenn Bech
 */
@Profile({"staging"})
@Configuration
public class ElmaStagingConfig {
    private static final String ELMA_ENDPOINT_KEY = "bdxr-transport-altinn";
    private static final TransportProfile TRANSPORT_PROFILE_ALTINN = new TransportProfile(ELMA_ENDPOINT_KEY);

    @Bean
    public TransportProfile getTransportProfile() {
        return TRANSPORT_PROFILE_ALTINN;
    }

    @Bean
    public LookupClient getElmaLookupClient() {
        return LookupClientBuilder.forTest().locator(new BusdoxLocator(DynamicLocator.OPENPEPPOL_TEST)).endpointCertificateValidator(null).providerCertificateValidator(null).build();
    }
}

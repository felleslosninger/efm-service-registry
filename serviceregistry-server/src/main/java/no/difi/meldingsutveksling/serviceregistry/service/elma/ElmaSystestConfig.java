package no.difi.meldingsutveksling.serviceregistry.service.elma;

import no.difi.vefa.peppol.common.lang.PeppolLoadingException;
import no.difi.vefa.peppol.common.model.TransportProfile;
import no.difi.vefa.peppol.lookup.LookupClient;
import no.difi.vefa.peppol.lookup.LookupClientBuilder;
import no.difi.vefa.peppol.lookup.locator.StaticLocator;
import no.difi.vefa.peppol.mode.Mode;
import no.difi.vefa.peppol.security.util.EmptyCertificateValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile({"itest", "systest"})
public class ElmaSystestConfig {
    private static final String ELMA_ENDPOINT_KEY = "bdxr-transport-altinn-systemtest";
    private static final TransportProfile TRANSPORT_PROFILE_ALTINN_SYSTEST = TransportProfile.of(ELMA_ENDPOINT_KEY);

    @Bean
    public TransportProfile getTransportProfile() {
        return TRANSPORT_PROFILE_ALTINN_SYSTEST;
    }

    @Bean
    public LookupClient getElmaLookupClient() throws PeppolLoadingException {
        Mode mode = Mode.of(Mode.TEST);
        return LookupClientBuilder.forTest()
                .locator(new StaticLocator("http://test-smp.difi.no.publisher.acc.edelivery.tech.ec.europa.eu"))
                .certificateValidator(EmptyCertificateValidator.INSTANCE)
                .build();
    }
}

package no.difi.meldingsutveksling.serviceregistry.client.brreg;

import no.difi.meldingsutveksling.serviceregistry.config.ServiceregistryProperties;
import no.difi.meldingsutveksling.serviceregistry.domain.BrregEnhet;
import no.difi.meldingsutveksling.serviceregistry.domain.BrregMockEnhet;
import no.difi.meldingsutveksling.serviceregistry.domain.BrregOrganisasjonsform;
import no.difi.meldingsutveksling.serviceregistry.service.brreg.dev.TestEnvironmentEnheter;

import java.net.URISyntaxException;
import java.util.Optional;

public class BrregMockClient implements BrregClient {

    TestEnvironmentEnheter enheter;
    ServiceregistryProperties properties;
    BrregClient clientImpl;

    public BrregMockClient(TestEnvironmentEnheter enheter, ServiceregistryProperties properties) throws URISyntaxException {
        this.enheter = enheter;
        this.properties = properties;
        this.clientImpl = new BrregClientImpl(properties.getBrreg().getEndpointURL().toURI());
    }

    @Override
    public Optional<BrregEnhet> getBrregEnhetByOrgnr(String orgnr) {
        Optional<BrregMockEnhet> enhet = enheter.getBrregEnhet(orgnr);
        Optional<BrregEnhet> brregEnhet = enhet.map(this::mapToBrregEnhet);
        if (brregEnhet.isPresent()) {
            return brregEnhet;
        }

        return clientImpl.getBrregEnhetByOrgnr(orgnr);
    }

    @Override
    public Optional<BrregEnhet> getBrregUnderenhetByOrgnr(String orgnr) {
        Optional<BrregMockEnhet> enhet = enheter.getBrregEnhet(orgnr);
        Optional<BrregEnhet> brregEnhet = enhet.map(this::mapToBrregEnhet);
        if (brregEnhet.isPresent()) {
            return brregEnhet;
        }

        return clientImpl.getBrregUnderenhetByOrgnr(orgnr);
    }

    private BrregEnhet mapToBrregEnhet(BrregMockEnhet mockEnhet) {
        return new BrregEnhet()
                .setOrganisasjonsnummer(mockEnhet.getOrgnr())
                .setNavn(mockEnhet.getName())
                .setOrganisasjonsform(new BrregOrganisasjonsform(mockEnhet.getOrgform()));
    }
}

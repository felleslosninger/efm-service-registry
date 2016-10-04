package no.difi.meldingsutveksling.serviceregistry.client.brreg;

import no.difi.meldingsutveksling.serviceregistry.model.BrregEnhet;
import no.difi.meldingsutveksling.serviceregistry.service.brreg.dev.TestEnvironmentEnheter;
import org.springframework.core.env.Environment;

import java.net.URI;
import java.util.Optional;

public class BrregMockClient implements BrregClient {

    TestEnvironmentEnheter enheter;
    Environment environment;
    BrregClient clientImpl;

    public BrregMockClient(TestEnvironmentEnheter enheter, Environment env) {
        this.enheter = enheter;
        this.environment = env;
        this.clientImpl = new BrregClientImpl(URI.create(environment.getProperty("brreg.enhetsregister.url")));
    }

    @Override
    public Optional<BrregEnhet> getBrregEnhetByOrgnr(String orgnr) {

        Optional<BrregEnhet> enhet = enheter.getBrregEnhet(orgnr);
        if (enhet.isPresent()) {
            return enhet;
        }

        return clientImpl.getBrregEnhetByOrgnr(orgnr);
    }
}

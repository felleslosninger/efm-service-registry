package no.difi.meldingsutveksling.serviceregistry.service.krr;

import no.difi.meldingsutveksling.ptp.KontaktInfo;
import no.difi.meldingsutveksling.ptp.OppslagstjenesteClient;
import no.difi.meldingsutveksling.serviceregistry.model.CitizenInfo;
import no.difi.meldingsutveksling.serviceregistry.model.EntityInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

@Service
public class KrrService {

    private final Environment environment;
    private OppslagstjenesteClient client;

    @Autowired
    KrrService(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    private void initClient() {
        client = new OppslagstjenesteClient(createConfiguration());

    }

    public KontaktInfo getCitizenInfo(String identifier) {

        return client.hentKontaktInformasjon(identifier);
    }

    private OppslagstjenesteClient.Configuration createConfiguration() {
        return new OppslagstjenesteClient.Configuration(
                environment.getProperty("krr.endpointURL"),
                environment.getProperty("jks.password"),
                environment.getProperty("jks.client.alias"),
                environment.getProperty("jks.server.alias")
        );
    }
}

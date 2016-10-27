package no.difi.meldingsutveksling.serviceregistry.service.krr;

import javax.annotation.PostConstruct;
import no.difi.meldingsutveksling.ptp.KontaktInfo;
import no.difi.meldingsutveksling.ptp.OppslagstjenesteClient;
import no.difi.meldingsutveksling.serviceregistry.config.ServiceregistryProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class KrrService {

    private ServiceregistryProperties properties;
    private OppslagstjenesteClient client;

    @Autowired
    KrrService(ServiceregistryProperties properties) {
        this.properties = properties;
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
                properties.getKrr().getEndpointURL().toString(),
                properties.getKrr().getClient().getPassword(),
                properties.getKrr().getClient().getAlias(),
                properties.getKrr().getServer().getAlias(),
                properties.getKrr().getClient().getKeystore(),
                properties.getKrr().getServer().getKeystore());
    }
}

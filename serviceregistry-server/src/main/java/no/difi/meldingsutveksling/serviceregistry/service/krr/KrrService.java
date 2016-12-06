package no.difi.meldingsutveksling.serviceregistry.service.krr;

import no.difi.meldingsutveksling.ptp.KontaktInfo;
import no.difi.meldingsutveksling.ptp.OppslagstjenesteClient;
import no.difi.meldingsutveksling.serviceregistry.config.ServiceregistryProperties;
import no.difi.meldingsutveksling.serviceregistry.krr.LookupParameters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

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

    public KontaktInfo getCitizenInfo(LookupParameters lookupParameters) {

        KontaktInfo kontaktInfo = client.hentKontaktInformasjon(lookupParameters);
        if (!kontaktInfo.canReceiveDigitalPost() || !kontaktInfo.hasMailbox()) {
            kontaktInfo.setPrintDetails(client.getPrintProviderDetails(lookupParameters));
        }
        return kontaktInfo;
    }

    private OppslagstjenesteClient.Configuration createConfiguration() {
        final OppslagstjenesteClient.Configuration configuration = new OppslagstjenesteClient.Configuration(
                properties.getKrr().getEndpointURL().toString(),
                properties.getKrr().getClient().getPassword(),
                properties.getKrr().getClient().getAlias(),
                properties.getKrr().getServer().getAlias(),
                properties.getKrr().getClient().getKeystore(),
                properties.getKrr().getServer().getKeystore());
        configuration.setPaaVegneAvEnabled(properties.getFeature().isPaaVegneAvOppslag());
        return configuration;
    }
}

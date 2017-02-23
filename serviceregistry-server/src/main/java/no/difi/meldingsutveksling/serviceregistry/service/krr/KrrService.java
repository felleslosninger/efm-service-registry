package no.difi.meldingsutveksling.serviceregistry.service.krr;

import no.difi.meldingsutveksling.ptp.KontaktInfo;
import no.difi.meldingsutveksling.ptp.OppslagstjenesteClient;
import no.difi.meldingsutveksling.serviceregistry.config.ServiceregistryProperties;
import no.difi.meldingsutveksling.serviceregistry.krr.KRRClient;
import no.difi.meldingsutveksling.serviceregistry.krr.KRRClientException;
import no.difi.meldingsutveksling.serviceregistry.krr.LookupParameters;
import no.difi.meldingsutveksling.serviceregistry.krr.PersonResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

@Service
public class KrrService {

    private ServiceregistryProperties properties;
    private OppslagstjenesteClient client;
    private KRRClient krrClient;

    @Autowired
    KrrService(ServiceregistryProperties properties) {
        this.properties = properties;
        this.krrClient = new KRRClient(properties.getKrr().getEndpointURL());
    }

    @PostConstruct
    private void initClient() {
        client = new OppslagstjenesteClient(createConfiguration());
    }

    public KontaktInfo getCitizenInfo(LookupParameters lookupParameters) {

        KontaktInfo kontaktInfo = client.hentKontaktInformasjon(lookupParameters);
        if (kontaktInfo.canReceiveDigitalPost() &&
                (kontaktInfo.isNotifiable() || !lookupParameters.isObligatedToBeNotified())) {
            return kontaktInfo;
        }
        kontaktInfo.setPrintDetails(client.getPrintProviderDetails(lookupParameters));
        return kontaktInfo;
    }

    public PersonResource getCizitenInfo(String identifier, String token) throws KRRClientException {

        return krrClient.getPersonResource(identifier, token);
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

    public void setClient(OppslagstjenesteClient client) {
        this.client = client;
    }
}

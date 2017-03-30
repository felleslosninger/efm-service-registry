package no.difi.meldingsutveksling.serviceregistry.service.krr;

import no.difi.meldingsutveksling.ptp.OppslagstjenesteClient;
import no.difi.meldingsutveksling.ptp.PrintProviderDetails;
import no.difi.meldingsutveksling.serviceregistry.config.ServiceregistryProperties;
import no.difi.meldingsutveksling.serviceregistry.krr.*;
import no.difi.move.common.oauth.KeystoreHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

@Service
public class KrrService {

    private ServiceregistryProperties properties;
    private KeystoreHelper keystoreHelper;
    private OppslagstjenesteClient client;
    private KRRClient krrClient;
    private DSFClient dsfClient;

    @Autowired
    KrrService(ServiceregistryProperties properties, KeystoreHelper keystoreHelper) {
        this.properties = properties;
        this.krrClient = new KRRClient(properties.getKrr().getEndpointURL(), keystoreHelper);
        this.dsfClient = new DSFClient(properties.getKrr().getDsfEndpointURL(), keystoreHelper);
    }

    @PostConstruct
    private void initClient() {
        client = new OppslagstjenesteClient(createConfiguration());
    }

    public PersonResource getCizitenInfo(LookupParameters params) throws
            KRRClientException {
        PersonResource personResource = krrClient.getPersonResource(params.getIdentifier(), params.getToken());
        if (personResource.canReceiveDigitalPost() &&
                (personResource.isNotifiable() || !params.isObligatedToBeNotified())) {
            return personResource;
        }

        PrintProviderDetails printProviderDetails = client.getPrintProviderDetails(params.getClientOrgnr());
        personResource.setCertificate(printProviderDetails.getPemCertificate());
        personResource.setPrintPostkasseLeverandorAdr(printProviderDetails.getPostkasseleverandoerAdresse());

        return personResource;
    }

    public DSFResource getDSFInfo(String identifier, String token) throws KRRClientException {
        return dsfClient.getDSFResource(identifier, token);
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

    public void setKrrClient(KRRClient krrClient) {
        this.krrClient = krrClient;
    }

    public void setDsfClient(DSFClient dsfClient) {
        this.dsfClient = dsfClient;
    }
}

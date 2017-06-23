package no.difi.meldingsutveksling.serviceregistry.service.krr;

import no.difi.meldingsutveksling.serviceregistry.config.ServiceregistryProperties;
import no.difi.meldingsutveksling.serviceregistry.krr.*;
import no.difi.move.common.oauth.KeystoreHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class KrrService {

    private ServiceregistryProperties properties;
    private KRRClient krrClient;
    private DSFClient dsfClient;

    @Autowired
    KrrService(ServiceregistryProperties properties, KeystoreHelper keystoreHelper) {
        this.properties = properties;
        this.krrClient = new KRRClient(properties.getKrr().getEndpointURL(), keystoreHelper);
        this.dsfClient = new DSFClient(properties.getKrr().getDsfEndpointURL(), keystoreHelper);
    }

    public PersonResource getCizitenInfo(LookupParameters params) throws
            KRRClientException {
        PersonResource personResource = krrClient.getPersonResource(params.getIdentifier(), params.getToken());
        if (personResource.canReceiveDigitalPost() &&
                (personResource.isNotifiable() || !params.isObligatedToBeNotified())) {
            return personResource;
        }

        setPrintDetails(personResource);
        return personResource;
    }

    void setPrintDetails(PersonResource personResource) {
        personResource.setCertificate(properties.getKrr().getPrintCertificate());
        personResource.setPrintPostkasseLeverandorAdr(properties.getKrr().getPrintAdress());
    }

    public DSFResource getDSFInfo(String identifier, String token) throws KRRClientException {
        return dsfClient.getDSFResource(identifier, token);
    }

    public void setKrrClient(KRRClient krrClient) {
        this.krrClient = krrClient;
    }

    public void setDsfClient(DSFClient dsfClient) {
        this.dsfClient = dsfClient;
    }
}

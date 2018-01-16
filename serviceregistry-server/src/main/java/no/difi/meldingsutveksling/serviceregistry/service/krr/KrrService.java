package no.difi.meldingsutveksling.serviceregistry.service.krr;

import no.difi.meldingsutveksling.serviceregistry.config.ServiceregistryProperties;
import no.difi.meldingsutveksling.serviceregistry.krr.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class KrrService {

    private ServiceregistryProperties properties;
    private KRRClient krrClient;
    private DSFClient dsfClient;

    @Autowired
    KrrService(ServiceregistryProperties properties) {
        this.properties = properties;
        this.krrClient = new KRRClient(properties.getKrr().getEndpointURL());
        this.dsfClient = new DSFClient(properties.getKrr().getDsfEndpointURL());
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

    public Optional<DSFResource> getDSFInfo(String identifier, String token) throws KRRClientException {
        return dsfClient.getDSFResource(identifier, token);
    }

    public void setKrrClient(KRRClient krrClient) {
        this.krrClient = krrClient;
    }

    public void setDsfClient(DSFClient dsfClient) {
        this.dsfClient = dsfClient;
    }
}

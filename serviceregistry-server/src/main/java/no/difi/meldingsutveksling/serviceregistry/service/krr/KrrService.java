package no.difi.meldingsutveksling.serviceregistry.service.krr;

import no.difi.meldingsutveksling.serviceregistry.CacheConfig;
import no.difi.meldingsutveksling.serviceregistry.config.ServiceregistryProperties;
import no.difi.meldingsutveksling.serviceregistry.krr.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
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

    @Cacheable(CacheConfig.KRR_CACHE)
    public PersonResource getCitizenInfo(LookupParameters params) throws KRRClientException {
        return krrClient.getPersonResource(params.getIdentifier(), params.getToken());
    }

    @Cacheable(CacheConfig.DSF_CACHE)
    public Optional<DSFResource> getDSFInfo(LookupParameters params) throws DsfLookupException {
        return dsfClient.getDSFResource(params.getIdentifier(), params.getToken());
    }

    public void setPrintDetails(PersonResource personResource) {
        personResource.setCertificate(properties.getKrr().getPrintCertificate());
        personResource.setPrintPostkasseLeverandorAdr(properties.getKrr().getPrintAdress());
    }
}

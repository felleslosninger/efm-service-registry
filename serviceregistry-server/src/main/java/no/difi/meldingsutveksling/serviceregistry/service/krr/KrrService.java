package no.difi.meldingsutveksling.serviceregistry.service.krr;

import lombok.SneakyThrows;
import no.difi.meldingsutveksling.serviceregistry.CacheConfig;
import no.difi.meldingsutveksling.serviceregistry.config.ServiceregistryProperties;
import no.difi.meldingsutveksling.serviceregistry.krr.*;
import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.util.Optional;

@Service
public class KrrService {

    private final ServiceregistryProperties properties;
    private final KRRClient krrClient;
    private final DSFClient dsfClient;

    @SneakyThrows
    public KrrService(ServiceregistryProperties srProps, OAuth2ResourceServerProperties resourceServerProperties) {
        URL jwkUrl = new URL(resourceServerProperties.getJwt().getJwkSetUri());
        this.properties = srProps;
        this.krrClient = new KRRClient(srProps.getKrr().getEndpointURL(), jwkUrl);
        this.dsfClient = new DSFClient(srProps.getKrr().getDsfEndpointURL(), jwkUrl);
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

package no.difi.meldingsutveksling.serviceregistry.service.krr;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import no.difi.meldingsutveksling.serviceregistry.config.ServiceregistryProperties;
import no.difi.meldingsutveksling.serviceregistry.krr.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Service
public class KrrService {

    private ServiceregistryProperties properties;
    private KRRClient krrClient;
    private DSFClient dsfClient;

    private LoadingCache<LookupParameters, PersonResource> krrCache;
    private LoadingCache<LookupParameters, Optional<DSFResource>> dsfCache;

    @Autowired
    KrrService(ServiceregistryProperties properties) {
        this.properties = properties;
        this.krrClient = new KRRClient(properties.getKrr().getEndpointURL());
        this.dsfClient = new DSFClient(properties.getKrr().getDsfEndpointURL());

        this.krrCache = CacheBuilder.newBuilder()
                .expireAfterWrite(1, TimeUnit.HOURS)
                .build(new CacheLoader<LookupParameters, PersonResource>() {
            @Override
            public PersonResource load(LookupParameters params) throws Exception {
                return loadCizitenInfo(params);
            }
        });
        this.dsfCache = CacheBuilder.newBuilder()
                .expireAfterWrite(1, TimeUnit.HOURS)
                .build(new CacheLoader<LookupParameters, Optional<DSFResource>>() {
                    @Override
                    public Optional<DSFResource> load(LookupParameters params) throws Exception {
                        return loadDSFInfo(params);
                    }
                });
    }

    public PersonResource getCizitenInfo(LookupParameters params) throws KRRClientException {
        try {
            return this.krrCache.get(params);
        } catch (ExecutionException e) {
            throw new KRRClientException(e);
        }
    }

    private PersonResource loadCizitenInfo(LookupParameters params) throws KRRClientException {
        return krrClient.getPersonResource(params.getIdentifier(), params.getToken());
    }

    public void setPrintDetails(PersonResource personResource) {
        personResource.setCertificate(properties.getKrr().getPrintCertificate());
        personResource.setPrintPostkasseLeverandorAdr(properties.getKrr().getPrintAdress());
    }

    public Optional<DSFResource> getDSFInfo(LookupParameters params) throws KRRClientException {
        try {
            return this.dsfCache.get(params);
        } catch (ExecutionException e) {
            throw new KRRClientException(e);
        }
    }

    private Optional<DSFResource> loadDSFInfo(LookupParameters params) throws KRRClientException {
        return dsfClient.getDSFResource(params.getIdentifier(), params.getToken());
    }
}

package no.difi.meldingsutveksling.serviceregistry.service.krr;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.serviceregistry.CacheConfig;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryException;
import no.difi.meldingsutveksling.serviceregistry.config.ServiceregistryProperties;
import no.difi.meldingsutveksling.serviceregistry.krr.*;
import org.apache.commons.io.IOUtils;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Service
@Slf4j
public class KontaktInfoService {

    private final ServiceregistryProperties properties;
    private final KRRClient krrClient;
    private final DSFClient dsfClient;

    @SneakyThrows
    public KontaktInfoService(ServiceregistryProperties srProps) {
        this.properties = srProps;
        this.krrClient = new KRRClient(srProps.getKrr().getEndpointURL());
        this.dsfClient = new DSFClient(srProps.getKrr().getDsfEndpointURL());
    }

    @Cacheable(CacheConfig.KRR_CACHE)
    public PersonResource getCitizenInfo(LookupParameters params) throws KontaktInfoException {
        return krrClient.getPersonResource(params.getIdentifier(), params.getToken());
    }

    @Cacheable(CacheConfig.DSF_CACHE)
    public Optional<DSFResource> getDSFInfo(LookupParameters params) throws KontaktInfoException {
        return dsfClient.getDSFResource(params.getIdentifier(), params.getToken());
    }

    public void setPrintDetails(PersonResource personResource) {
        try {
            personResource.setCertificate(IOUtils.toString(properties.getKrr().getPrintCertificate().getInputStream(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            log.error(String.format("Could not read certificate from %s", properties.getKrr().getPrintCertificate().toString()), e);
            throw new ServiceRegistryException(e);
        }
        personResource.setPrintPostkasseLeverandorAdr(properties.getKrr().getPrintAdress());
    }
}

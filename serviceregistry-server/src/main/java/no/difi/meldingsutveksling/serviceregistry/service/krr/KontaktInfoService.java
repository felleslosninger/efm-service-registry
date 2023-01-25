package no.difi.meldingsutveksling.serviceregistry.service.krr;

import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.serviceregistry.CacheConfig;
import no.difi.meldingsutveksling.serviceregistry.exceptions.ServiceRegistryException;
import no.difi.meldingsutveksling.serviceregistry.config.ServiceregistryProperties;
import no.difi.meldingsutveksling.serviceregistry.krr.*;
import org.apache.commons.io.IOUtils;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import no.difi.meldingsutveksling.serviceregistry.freg.client.FregGatewayClient;
import no.difi.meldingsutveksling.serviceregistry.freg.domain.FregGatewayEntity;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class KontaktInfoService {

    private final ServiceregistryProperties properties;
    private final KRRClient krrClient;
    private final DsfClient dsfClient;
    private final FregGatewayClient fregGatewayClient;

    @Cacheable(CacheConfig.KRR_CACHE)
    @Timed(value = "krr.client.timer", description = "Timer for KRR client")
    @Retryable
    public PersonResource getCitizenInfo(LookupParameters params) throws KontaktInfoException {
        if (params.getToken().getIssuer().toString().equals(properties.getAuth().getMaskinportenIssuer())) {
            return krrClient.getPersonResource(params, properties.getKrr().getMpEndpointUri());
        }
        return krrClient.getPersonResource(params, properties.getKrr().getOidcEndpointUri());
    }

//    @Cacheable(CacheConfig.DSF_CACHE)
//    @Timed(value = "dsf.client.timer", description = "Timer for DSF client")
//    @Retryable
//    public Optional<DsfResource> getDsfInfo(LookupParameters params) throws KontaktInfoException {
//        if (params.getToken().getIssuer().toString().equals(properties.getAuth().getMaskinportenIssuer())) {
//            return dsfClient.getDSFResource(params, properties.getKrr().getMpDsfEndpointUri());
//        }
//        return dsfClient.getDSFResource(params, properties.getKrr().getOidcDsfEndpointUri());
//    }

    @Cacheable(CacheConfig.DSF_CACHE)
    @Timed(value = "dsf.client.timer", description = "Timer for DSF client")
    @Retryable
    public Optional<FregGatewayEntity.Address.Response> getFregAdress(LookupParameters params){
//        if (params.getToken().getIssuer().toString().equals(properties.getAuth().getMaskinportenIssuer())) {
//            return dsfClient.getDSFResource(params, properties.getKrr().getMpDsfEndpointUri());
//        }
        return fregGatewayClient.getPersonAdress(params.getIdentifier());
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

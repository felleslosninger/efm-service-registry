package no.difi.meldingsutveksling.serviceregistry.service.krr;

import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.serviceregistry.CacheConfig;
import no.difi.meldingsutveksling.serviceregistry.config.ServiceregistryProperties;
import no.difi.meldingsutveksling.serviceregistry.freg.client.DefaultFregGatewayClient;
import no.difi.meldingsutveksling.serviceregistry.freg.domain.FregGatewayEntity;
import no.difi.meldingsutveksling.serviceregistry.freg.exception.NotFoundInMfGatewayException;
import no.difi.meldingsutveksling.serviceregistry.krr.KRRClient;
import no.difi.meldingsutveksling.serviceregistry.krr.KontaktInfoException;
import no.difi.meldingsutveksling.serviceregistry.krr.LookupParameters;
import no.difi.meldingsutveksling.serviceregistry.krr.PersonResource;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class KontaktInfoService {

    private final ServiceregistryProperties properties;
    private final KRRClient krrClient;
    private final DefaultFregGatewayClient defaultFregGatewayClient;
    private final PrintService printService;

    @Cacheable(CacheConfig.KRR_CACHE)
    @Timed(value = "krr.client.timer", description = "Timer for KRR client")
    @Retryable
    public PersonResource getCitizenInfo(LookupParameters params) throws KontaktInfoException {
        if (params.getToken().getIssuer().toString().equals(properties.getAuth().getMaskinportenIssuer())) {
            return krrClient.getPersonResource(params, properties.getKrr().getMpEndpointUri());
        }
        return krrClient.getPersonResource(params, properties.getKrr().getOidcEndpointUri());
    }

    @Cacheable(CacheConfig.DSF_CACHE)
    @Timed(value = "dsf.client.timer", description = "Timer for DSF client")
    @Retryable
    public Optional<FregGatewayEntity.Address.Response> getFregAdress(LookupParameters params) throws NotFoundInMfGatewayException {
        return defaultFregGatewayClient.getPersonAdress(params.getIdentifier());
    }

    public void setPrintDetails(PersonResource personResource) {
        PrintResponse printDetails = printService.getPrintDetails();
        personResource.setCertificate(printDetails.getX509Sertifikat());
        personResource.setPrintPostkasseLeverandorAdr(printDetails.getPostkasseleverandoerAdresse());
    }
}

package no.difi.meldingsutveksling.serviceregistry.freg.mock;

import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.serviceregistry.config.ServiceregistryProperties;
import no.difi.meldingsutveksling.serviceregistry.freg.client.DefaultFregGatewayClient;
import no.difi.meldingsutveksling.serviceregistry.freg.domain.FregGatewayEntity;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@Slf4j
@Profile({"dev-local", "dev", "test", "yt"})
public class DefaultFregClientWithMock extends DefaultFregGatewayClient {

    public DefaultFregClientWithMock(ServiceregistryProperties properties) {
        super(properties);
    }

    @Override
    public Optional<FregGatewayEntity.Address.Response> getPersonAdress(String pid) {
        try {
            return super.getAddressFromFreg(pid);
        } catch (HttpClientErrorException.NotFound e) {
            log.info("User {} not found in Tenor testdatasøk. Returning mock user", pid);
            return Optional.of(FregGatewayEntity.Address.Response.builder()
                    .personIdentifikator(pid)
                    .navn(FregGatewayEntity.Address.Navn.builder()
                            .fornavn("Raff")
                            .etternavn("Raffen")
                            .build())
                    .postadresse(FregGatewayEntity.Address.PostAdresse.builder()
                            .adresselinje(new ArrayList<>(List.of("Portveien 2")))
                            .postnummer("0468")
                            .poststed("Oslo")
                            .landkode("Norge")
                            .build())
                    .build());
        } catch (URISyntaxException e) {
            log.debug("Invalid URI encountered in Folkeregisteret client with mock");
            return Optional.empty();
        }
    }
}
package no.difi.meldingsutveksling.serviceregistry.freg.mock;

import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.serviceregistry.config.ServiceregistryProperties;
import no.difi.meldingsutveksling.serviceregistry.freg.client.DefaultFregGatewayClient;
import no.difi.meldingsutveksling.serviceregistry.freg.domain.FregGatewayEntity;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@Slf4j
@Profile({"dev-local", "dev", "staging", "yt"})
public class DefaultFregClientWithMock extends DefaultFregGatewayClient {

    public DefaultFregClientWithMock(ServiceregistryProperties properties) {
        super(properties);
    }

    @Override
    public Optional<FregGatewayEntity.Address.Response> getPersonAdress(String pid) {
        try {
            return super.getPersonAdress(pid);
        } catch (HttpClientErrorException.NotFound e) {
            log.info("User not found in Tenor testdatasøk. Returning mock user", e);
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
        }
    }
}
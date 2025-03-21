package no.difi.meldingsutveksling.serviceregistry.freg.mock;

import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.serviceregistry.freg.client.FregGatewayClient;
import no.difi.meldingsutveksling.serviceregistry.freg.domain.FregGatewayEntity;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@Slf4j
@ConditionalOnProperty(prefix = "difi.move.freg", name = "enabled", havingValue = "false")
public class FregClientMock implements FregGatewayClient {

    public Optional<FregGatewayEntity.Address.Response> getPersonAdress(String pid) {
        log.info("Returning mock user with pid {}", pid);
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
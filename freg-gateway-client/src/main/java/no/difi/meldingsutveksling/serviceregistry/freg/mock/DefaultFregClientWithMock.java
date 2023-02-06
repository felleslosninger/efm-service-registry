package no.difi.meldingsutveksling.serviceregistry.freg.mock;

import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.serviceregistry.config.ServiceregistryProperties;
import no.difi.meldingsutveksling.serviceregistry.freg.client.DefaultFregGatewayClient;
import no.difi.meldingsutveksling.serviceregistry.freg.domain.FregGatewayEntity;
import no.difi.meldingsutveksling.serviceregistry.freg.exception.NotFoundInMfGatewayException;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Optional;

@Component
@Slf4j
@Profile({"dev", "staging", "yt"})
public class DefaultFregClientWithMock extends DefaultFregGatewayClient {

    public DefaultFregClientWithMock(ServiceregistryProperties properties) {
        super(properties);
    }

    @Override
    public Optional<FregGatewayEntity.Address.Response> getPersonAdress(String pid) throws NotFoundInMfGatewayException {
        ArrayList<String> adresselinje = new ArrayList<>();
        adresselinje.add("Portveien 2");
        return super.getPersonAdress(pid)
                .or(() -> Optional.of(FregGatewayEntity.Address.Response.builder()
                    .personIdentifikator(pid)
                    .navn(FregGatewayEntity.Address.Navn.builder()
                            .fornavn("Raff")
                            .etternavn("Raffen")
                            .build())
                    .postadresse(FregGatewayEntity.Address.PostAdresse.builder()
                            .adresselinje(adresselinje)
                            .postnummer("0468")
                            .poststed("Oslo")
                            .landkode("Norge")
                            .build())
                    .build()));
    }
}
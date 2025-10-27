package no.difi.meldingsutveksling.serviceregistry.freg.client;

import no.difi.meldingsutveksling.serviceregistry.freg.domain.FregGatewayEntity;

import java.util.Optional;

public interface FregGatewayClient {
    Optional<FregGatewayEntity.Address.Response> getPersonAdress(String pid);
}

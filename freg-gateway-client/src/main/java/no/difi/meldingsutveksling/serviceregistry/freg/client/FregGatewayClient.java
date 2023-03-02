package no.difi.meldingsutveksling.serviceregistry.freg.client;

import no.difi.meldingsutveksling.serviceregistry.freg.domain.FregGatewayEntity;
import no.difi.meldingsutveksling.serviceregistry.freg.exception.NotFoundInMfGatewayException;

import java.util.Optional;

public interface FregGatewayClient {
    Optional<FregGatewayEntity.Address.Response> getPersonAdress(String pid) throws NotFoundInMfGatewayException;
}

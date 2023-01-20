package no.difi.meldingsutveksling.serviceregistry.client.freg;

import no.difi.meldingsutveksling.serviceregistry.krr.FregGatewayResource;
import no.difi.meldingsutveksling.serviceregistry.krr.KontaktInfoException;
import no.difi.meldingsutveksling.serviceregistry.krr.LookupParameters;

import java.util.Optional;

public interface FregClient {
    Optional<FregGatewayResource> getFregPersonByPid(LookupParameters lookupParameters) throws KontaktInfoException;
}

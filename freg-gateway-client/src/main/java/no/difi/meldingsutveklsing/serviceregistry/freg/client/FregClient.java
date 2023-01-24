package no.difi.meldingsutveklsing.serviceregistry.freg.client;

import no.difi.meldingsutveklsing.serviceregistry.freg.domain.Person;
import no.difi.meldingsutveklsing.serviceregistry.freg.exception.FregGatewayException;

import java.util.Optional;

public interface FregClient {
    Optional<Person> getFregPersonByPid(String pid) throws FregGatewayException;
}

package no.difi.meldingsutveksling.serviceregistry.krr;

import java.net.URI;
import java.util.Optional;

public interface DsfClient {

    Optional<DsfResource> getDSFResource(LookupParameters params, URI endpointUri) throws KontaktInfoException;

}

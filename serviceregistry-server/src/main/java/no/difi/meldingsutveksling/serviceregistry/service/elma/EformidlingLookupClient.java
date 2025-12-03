package no.difi.meldingsutveksling.serviceregistry.service.elma;

import network.oxalis.vefa.peppol.lookup.LookupClient;
import network.oxalis.vefa.peppol.lookup.LookupClientBuilder;

public class EformidlingLookupClient extends LookupClient {

    protected EformidlingLookupClient(LookupClientBuilder builder) {
        super(builder);
    }

}

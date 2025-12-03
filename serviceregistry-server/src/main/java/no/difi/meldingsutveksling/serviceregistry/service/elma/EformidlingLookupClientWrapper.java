package no.difi.meldingsutveksling.serviceregistry.service.elma;

import lombok.Getter;
import network.oxalis.vefa.peppol.lookup.LookupClient;

public class EformidlingLookupClientWrapper {

    @Getter
    private final LookupClient lookupClient;

    public EformidlingLookupClientWrapper(LookupClient lookupClient) {
        this.lookupClient = lookupClient;
    }

}
package no.difi.meldingsutveksling.serviceregistry.service.elma;

import lombok.Getter;
import network.oxalis.vefa.peppol.common.model.DocumentTypeIdentifier;
import network.oxalis.vefa.peppol.common.model.ParticipantIdentifier;
import network.oxalis.vefa.peppol.common.model.ServiceMetadata;
import network.oxalis.vefa.peppol.lookup.LookupClient;
import network.oxalis.vefa.peppol.lookup.api.LookupException;
import network.oxalis.vefa.peppol.security.lang.PeppolSecurityException;

public class EformidlingLookupClientWrapper {

    @Getter
    private final LookupClient lookupClient;

    public EformidlingLookupClientWrapper(LookupClient lookupClient) {
        this.lookupClient = lookupClient;
    }

    public ServiceMetadata getServiceMetadata(ParticipantIdentifier participantIdentifier, DocumentTypeIdentifier documentTypeIdentifier)
            throws LookupException, PeppolSecurityException {
        return lookupClient.getServiceMetadata(participantIdentifier, documentTypeIdentifier);
    }

}
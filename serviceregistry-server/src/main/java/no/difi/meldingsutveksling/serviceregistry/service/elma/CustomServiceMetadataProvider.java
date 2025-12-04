package no.difi.meldingsutveksling.serviceregistry.service.elma;

import network.oxalis.vefa.peppol.common.model.DocumentTypeIdentifier;
import network.oxalis.vefa.peppol.common.model.ParticipantIdentifier;
import network.oxalis.vefa.peppol.lookup.api.MetadataProvider;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class CustomServiceMetadataProvider implements MetadataProvider {

    @Override
    public List<URI> resolveDocumentIdentifiers(URI location, ParticipantIdentifier participantIdentifier) {
        String basePath = normalizeBasePath(location.toString());
        URI baseUri = URI.create(basePath + "/");
        URI resolvedUri = baseUri.resolve(participantIdentifier.urlencoded());
        return List.of(resolvedUri);
    }

    @Override
    public List<URI> resolveServiceMetadata(URI location,
                                            ParticipantIdentifier participantIdentifier,
                                            DocumentTypeIdentifier documentTypeIdentifier) {
        List<URI> resolvedServiceMetaDataURIList = new ArrayList<>();
        String docSchemeIdentifier = documentTypeIdentifier.getScheme().getIdentifier();

        if (DocumentTypeIdentifier.DOCUMENT_TYPE_SCHEME_BUSDOX_DOCID_QNS.equals(docSchemeIdentifier)) {
            addResolvedUri(location, participantIdentifier, documentTypeIdentifier, resolvedServiceMetaDataURIList);
        }

        return resolvedServiceMetaDataURIList;
    }

    private void addResolvedUri(URI location,
                                ParticipantIdentifier participantIdentifier,
                                DocumentTypeIdentifier documentTypeIdentifier,
                                List<URI> resolvedServiceMetaDataURIList) {
        addResolvedServiceURI(location, participantIdentifier, documentTypeIdentifier.urlencoded(), resolvedServiceMetaDataURIList);
    }

    private void addResolvedServiceURI(URI location,
                                       ParticipantIdentifier participantIdentifier,
                                       String documentTypeIdentifierUrlEncoded,
                                       List<URI> resolvedServiceMetaDataURIList) {

        String basePath = normalizeBasePath(location.toString());
        URI baseUri = URI.create(basePath + "/");

        URI resolvedUri = baseUri.resolve(
                String.format("%s/services/%s",
                        participantIdentifier.urlencoded(),
                        documentTypeIdentifierUrlEncoded)
        );

        resolvedServiceMetaDataURIList.add(resolvedUri);
    }

    private String normalizeBasePath(String location) {
        String basePath = location.trim();
        while (basePath.endsWith("/")) {
            basePath = basePath.substring(0, basePath.length() - 1);
        }
        return basePath;
    }
}

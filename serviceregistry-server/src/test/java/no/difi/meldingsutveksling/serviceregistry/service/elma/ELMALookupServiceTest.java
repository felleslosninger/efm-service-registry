package no.difi.meldingsutveksling.serviceregistry.service.elma;

import network.oxalis.vefa.peppol.common.model.DocumentTypeIdentifier;
import network.oxalis.vefa.peppol.common.model.ParticipantIdentifier;
import network.oxalis.vefa.peppol.common.model.ServiceMetadata;
import network.oxalis.vefa.peppol.lookup.LookupClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ELMALookupServiceTest {

    private LookupClient peppolLookupClient;

    private EformidlingLookupClient eFormidlingLookupClient;

    private ELMALookupService target;

    private static final String ORG_NUMBER = "0192:123456789";
    private static final String EFORMIDLING_DOC_ID = "urn:no:difi:arkivmelding:xsd::arkivmelding";
    private static final String PEPPOL_DOC_ID = "urn:fdc:digdir.no:2020:innbyggerpost:xsd::innbyggerpost##urn:fdc:digdir.no:2020:innbyggerpost:schema:digital::1.0";

    @BeforeEach
    void setUp() {
        peppolLookupClient = mock(LookupClient.class);
        eFormidlingLookupClient= mock(EformidlingLookupClient.class);
        target = new ELMALookupService(peppolLookupClient, eFormidlingLookupClient);
    }

    @Test
    void lookup_shouldUseEformidlingClient_foreFormidlingDocumentIdentifier() throws Exception {
        // Arrange
        Set<String> documentIds = Set.of(EFORMIDLING_DOC_ID);
        List<DocumentTypeIdentifier> registeredIds = List.of(DocumentTypeIdentifier.of(EFORMIDLING_DOC_ID));

        lenient().when(peppolLookupClient.getDocumentIdentifiers(any())).thenReturn(registeredIds);
        lenient().when(eFormidlingLookupClient.getServiceMetadata(any(), any())).thenReturn(mock(ServiceMetadata.class));

        // Act
        target.lookup(ORG_NUMBER, documentIds);

        // Assert
        verify(eFormidlingLookupClient, times(1)).getServiceMetadata(
                eq(ParticipantIdentifier.of(ORG_NUMBER)),
                eq(DocumentTypeIdentifier.of(EFORMIDLING_DOC_ID))
        );
        verify(peppolLookupClient, never()).getServiceMetadata(any(), any());
    }

    @Test
    void lookup_shouldUsePeppolClient_forNoneFormidlingDocumentIdentifier() throws Exception {
        // Arrange
        Set<String> documentIds = Set.of(PEPPOL_DOC_ID);
        List<DocumentTypeIdentifier> registeredIds = List.of(DocumentTypeIdentifier.of(PEPPOL_DOC_ID));

        lenient().when(peppolLookupClient.getDocumentIdentifiers(any())).thenReturn(registeredIds);
        lenient().when(peppolLookupClient.getServiceMetadata(any(), any())).thenReturn(mock(ServiceMetadata.class));

        // Act
        target.lookup(ORG_NUMBER, documentIds);

        // Assert
        verify(peppolLookupClient, times(1)).getServiceMetadata(
                eq(ParticipantIdentifier.of(ORG_NUMBER)),
                eq(DocumentTypeIdentifier.of(PEPPOL_DOC_ID))
        );
        verify(eFormidlingLookupClient, never()).getServiceMetadata(any(), any());
    }

    @Test
    void lookup_shouldUseBothClients_forMixedDocumentIdentifiers() throws Exception {
        // Arrange
        Set<String> documentIds = Set.of(EFORMIDLING_DOC_ID, PEPPOL_DOC_ID);
        List<DocumentTypeIdentifier> registeredIds = List.of(
                DocumentTypeIdentifier.of(EFORMIDLING_DOC_ID),
                DocumentTypeIdentifier.of(PEPPOL_DOC_ID)
        );

        lenient().when(peppolLookupClient.getDocumentIdentifiers(any())).thenReturn(registeredIds);
        when(peppolLookupClient.getServiceMetadata(any(), eq(DocumentTypeIdentifier.of(PEPPOL_DOC_ID)))).thenReturn(mock(ServiceMetadata.class));
        when(eFormidlingLookupClient.getServiceMetadata(any(), eq(DocumentTypeIdentifier.of(EFORMIDLING_DOC_ID)))).thenReturn(mock(ServiceMetadata.class));

        // Act
        target.lookup(ORG_NUMBER, documentIds);

        // Assert
        verify(peppolLookupClient, times(1)).getServiceMetadata(any(), eq(DocumentTypeIdentifier.of(PEPPOL_DOC_ID)));
        verify(eFormidlingLookupClient, times(1)).getServiceMetadata(any(), eq(DocumentTypeIdentifier.of(EFORMIDLING_DOC_ID)));
    }
}

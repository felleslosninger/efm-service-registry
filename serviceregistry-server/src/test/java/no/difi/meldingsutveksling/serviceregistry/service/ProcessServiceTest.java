package no.difi.meldingsutveksling.serviceregistry.service;

import no.difi.meldingsutveksling.serviceregistry.domain.DocumentType;
import no.difi.meldingsutveksling.serviceregistry.domain.Process;
import no.difi.meldingsutveksling.serviceregistry.persistence.ProcessRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ProcessServiceTest {

    @InjectMocks
    private ProcessService target;

    @Mock
    private ProcessRepository repositoryMock;

    @Mock
    private DocumentTypeService documentTypeServiceMock;

    @Test
    public void update_ProcessNotFound_ResponseShouldBeFalse() {
        when(repositoryMock.findByIdentifier(anyString())).thenReturn(null);
        Process updatedProcess = createProcess("n/a", "n/a", null);

        boolean result = target.update("process", updatedProcess);

        assertFalse(result);
        verify(repositoryMock, never()).save(any(Process.class));
    }

    @Test
    public void update_ChangeServiceCode_ShouldBeSaved() {
        Process existingProcessMock = mock(Process.class);
        when(repositoryMock.findByIdentifier(anyString())).thenReturn(existingProcessMock);
        String newCode = "newCode";
        Process updatedProcess = createProcess(newCode, null, null);
        when(repositoryMock.save(existingProcessMock)).thenReturn(existingProcessMock);

        boolean result = target.update("process", updatedProcess);

        assertTrue(result);
        verify(repositoryMock).save(any(Process.class));
        verify(existingProcessMock).setServiceCode(newCode);
    }

    @Test
    public void update_ChangeServiceEditionCode_ShouldBeSaved() {
        Process existingProcessMock = mock(Process.class);
        when(repositoryMock.findByIdentifier(anyString())).thenReturn(existingProcessMock);
        String newCode = "newCode";
        Process updatedProcess = createProcess(null, newCode, null);
        when(repositoryMock.save(existingProcessMock)).thenReturn(existingProcessMock);

        boolean result = target.update("process", updatedProcess);

        assertTrue(result);
        verify(repositoryMock).save(any(Process.class));
        verify(existingProcessMock).setServiceEditionCode(newCode);
    }

    @Test
    public void update_ChangeDocumentTypes_ShouldBeSaved() {
        Process existingProcessMock = mock(Process.class);
        when(repositoryMock.findByIdentifier(any())).thenReturn(existingProcessMock);
        List<DocumentType> newTypes = new ArrayList<>();
        DocumentType documentTypeMock = mock(DocumentType.class);
        newTypes.add(documentTypeMock);
        when(documentTypeServiceMock.findByIdentifier(any())).thenReturn(Optional.of(documentTypeMock));
        Process updatedProcess = createProcess(null, null, newTypes);
        when(repositoryMock.save(existingProcessMock)).thenReturn(existingProcessMock);

        boolean result = target.update("process", updatedProcess);

        assertTrue(result);
        verify(repositoryMock).save(any(Process.class));
        verify(existingProcessMock).setDocumentTypes(newTypes);
    }

    @Test
    public void update_ProcessContainsNonExistingDocumentType_ShouldAddDocumentTypeAndSucceed() {
        Process existingProcessMock = mock(Process.class);
        when(repositoryMock.findByIdentifier(anyString())).thenReturn(existingProcessMock);
        List<DocumentType> newTypes = new ArrayList<>();
        DocumentType documentTypeMock = mock(DocumentType.class);
        newTypes.add(documentTypeMock);
        Process updatedProcess = createProcess(null, null, newTypes);
        when(repositoryMock.save(existingProcessMock)).thenReturn(existingProcessMock);

        boolean result = target.update("process", updatedProcess);

        assertTrue(result);
        verify(documentTypeServiceMock).add(documentTypeMock);
    }

    private static Process createProcess(String serviceCode, String serviceEditionCode, List<DocumentType> documentTypes) {
        Process updatedProcess = new Process();
        updatedProcess.setServiceCode(serviceCode);
        updatedProcess.setServiceEditionCode(serviceEditionCode);
        updatedProcess.setDocumentTypes(documentTypes);
        return updatedProcess;
    }
}

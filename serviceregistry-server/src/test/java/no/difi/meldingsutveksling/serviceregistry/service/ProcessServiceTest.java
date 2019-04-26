package no.difi.meldingsutveksling.serviceregistry.service;

import no.difi.meldingsutveksling.serviceregistry.model.DocumentType;
import no.difi.meldingsutveksling.serviceregistry.model.Process;
import no.difi.meldingsutveksling.serviceregistry.persistence.ProcessRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
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
        when(repositoryMock.findByIdentifier(anyString())).thenReturn(existingProcessMock);
        List<DocumentType> newTypes = new ArrayList<>();
        DocumentType documentTypeMock = mock(DocumentType.class);
        newTypes.add(documentTypeMock);
        when(documentTypeServiceMock.findByIdentifier(anyString())).thenReturn(Optional.of(documentTypeMock));
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
        when(documentTypeServiceMock.findByIdentifier(anyString())).thenReturn(Optional.empty());
        Process updatedProcess = createProcess(null, null, newTypes);
        when(repositoryMock.save(existingProcessMock)).thenReturn(existingProcessMock);

        boolean result = target.update("process", updatedProcess);

        assertTrue(result);
        verify(documentTypeServiceMock).add(documentTypeMock);
    }

    private Process createProcess(String serviceCode, String serviceEditionCode, List<DocumentType> documentTypes) {
        Process updatedProcess = new Process();
        updatedProcess.setServiceCode(serviceCode);
        updatedProcess.setServiceEditionCode(serviceEditionCode);
        updatedProcess.setDocumentTypes(documentTypes);
        return updatedProcess;
    }
}

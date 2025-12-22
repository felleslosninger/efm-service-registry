package no.difi.meldingsutveksling.serviceregistry.service;

import network.oxalis.vefa.peppol.common.model.TransportProfile;
import network.oxalis.vefa.peppol.lookup.LookupClient;
import no.difi.meldingsutveksling.serviceregistry.MoveServiceRegistryApplication;
import no.difi.meldingsutveksling.serviceregistry.client.brreg.BrregClient;
import no.difi.meldingsutveksling.serviceregistry.domain.DocumentType;
import no.difi.meldingsutveksling.serviceregistry.domain.Process;
import no.difi.meldingsutveksling.serviceregistry.domain.ProcessCategory;
import no.difi.meldingsutveksling.serviceregistry.persistence.DocumentTypeRepository;
import no.difi.meldingsutveksling.serviceregistry.persistence.ProcessRepository;
import no.difi.meldingsutveksling.serviceregistry.service.krr.KontaktInfoService;
import no.difi.meldingsutveksling.serviceregistry.service.virksert.VirkSertService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = MoveServiceRegistryApplication.class)
@ActiveProfiles("test")
public class ProcessAdministrationIT {

    @Autowired
    private ProcessService processService;

    @Autowired
    private ProcessRepository processRepository;

    @MockitoBean
    private VirkSertService virkSertService;

    @MockitoBean
    private LookupClient lookupClient;

    @MockitoBean
    private TransportProfile transportProfile;

    @MockitoBean
    private KontaktInfoService kontaktInfoService;

    @MockitoBean
    private BrregClient brregClient;

    @Autowired
    private DocumentTypeService documentTypeService;

    @Autowired
    private DocumentTypeRepository documentTypeRepository;

    @BeforeEach
    public void setUp() {
        processRepository.deleteAll();
        documentTypeRepository.deleteAll();
    }

    @Test
    public void addProcess_NewDocumentType_ProcessAndDocumentTypeShouldBeAdded() {
        Process process = createProcess("process", "service", "edition", "resource", ProcessCategory.ARKIVMELDING);
        ArrayList<DocumentType> documentTypes = new ArrayList<>();
        DocumentType documentType = createDocumentType("DocumentType");
        documentTypes.add(documentType);
        process.setDocumentTypes(documentTypes);

        processService.add(process);

        assertEquals(1, processRepository.count());
        assertEquals(1, documentTypeRepository.count());
    }

    @Test
    public void addProcess_DocumentTypeAlreadyExists_ProcessShouldBeAdded() {
        Process process = createProcess("process", "service", "edition", "resource", ProcessCategory.ARKIVMELDING);
        ArrayList<DocumentType> documentTypes = new ArrayList<>();
        DocumentType documentType = createDocumentType("DocumentType");
        documentTypes.add(documentType);
        process.setDocumentTypes(documentTypes);
        documentTypeRepository.save(documentType);

        assertEquals(1, documentTypeRepository.count());
        processService.add(process);

        assertEquals(1, processRepository.count());
        assertEquals(1, documentTypeRepository.count());
    }

    @Test
    public void updateProcess_NewDocumentType_ProcessAndDocumentTypeShouldBeAdded() {
        String processIdentifier = "process";
        Process process = createProcess(processIdentifier, "service", "edition", "resource", ProcessCategory.ARKIVMELDING);
        ArrayList<DocumentType> documentTypes = new ArrayList<>();
        DocumentType documentType1 = createDocumentType("DocumentType1");
        documentTypes.add(documentType1);
        process.setDocumentTypes(documentTypes);
        documentTypeRepository.save(documentType1);
        processRepository.save(process);
        DocumentType documentType2 = createDocumentType("DocumentType2");
        documentTypes.add(documentType2);

        processService.update(processIdentifier, process);

        assertEquals(1, processRepository.count());
        assertEquals(2, documentTypeRepository.count());
    }

    @Test
    public void updateProcess_DocumentTypeAlreadyExists_ProcessAndDocumentTypeShouldBeAdded() {
        String processIdentifier = "process";
        Process process = createProcess(processIdentifier, "service", "edition", "resource", ProcessCategory.ARKIVMELDING);
        ArrayList<DocumentType> documentTypes = new ArrayList<>();
        DocumentType documentType1 = createDocumentType("DocumentType1");
        documentTypes.add(documentType1);
        DocumentType documentType2 = createDocumentType("DocumentType2");
        documentTypeRepository.save(documentType1);
        documentTypeRepository.save(documentType2);
        process.setDocumentTypes(documentTypes);
        processRepository.save(process);
        documentTypes.add(documentType2);
        process.setDocumentTypes(documentTypes);

        processService.update(processIdentifier, process);

        assertEquals(1, processRepository.count());
        assertEquals(2, documentTypeRepository.count());
    }

    @Test
    public void deleteProcess_ProcessHasDocumentType_DocumentTypeShouldRemain() {
        Process process = createProcess("process", "service", "edition", "resource", ProcessCategory.ARKIVMELDING);
        ArrayList<DocumentType> documentTypes = new ArrayList<>();
        DocumentType documentType = createDocumentType("DocumentType");
        documentTypes.add(documentType);
        process.setDocumentTypes(documentTypes);
        documentTypeRepository.save(documentType);
        processRepository.save(process);

        processService.delete(process);

        assertEquals(0, processRepository.count());
        assertEquals(1, documentTypeRepository.count());
    }

    private DocumentType createDocumentType(String identifier) {
        DocumentType documentType = new DocumentType();
        documentType.setIdentifier(identifier);
        return documentType;
    }

    private Process createProcess(String identifier, String serviceCode, String serviceEditionCode, String resource, ProcessCategory category) {
        Process process = new Process();
        process.setIdentifier(identifier);
        process.setServiceCode(serviceCode);
        process.setServiceEditionCode(serviceEditionCode);
        process.setCategory(category);
        process.setResource(resource);
        return process;
    }

}

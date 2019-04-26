package no.difi.meldingsutveksling.serviceregistry.service;

import com.google.common.collect.Lists;
import no.difi.meldingsutveksling.serviceregistry.EntityNotFoundException;
import no.difi.meldingsutveksling.serviceregistry.config.ServiceregistryProperties;
import no.difi.meldingsutveksling.serviceregistry.model.DocumentType;
import no.difi.meldingsutveksling.serviceregistry.model.Process;
import no.difi.meldingsutveksling.serviceregistry.model.ProcessCategory;
import no.difi.meldingsutveksling.serviceregistry.persistence.ProcessRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ProcessService {

    private final ProcessRepository repository;
    private final ServiceregistryProperties props;
    private final DocumentTypeService documentTypeService;

    public ProcessService(ProcessRepository repository, ServiceregistryProperties props, DocumentTypeService documentTypeService) {
        this.repository = repository;
        this.props = props;
        this.documentTypeService = documentTypeService;
    }

    @Transactional(readOnly = true)
    public Optional<Process> findByIdentifier(String identifier) {
        return Optional.ofNullable(repository.findByIdentifier(identifier));
    }

    @Transactional
    public Process add(Process process) {
        List<DocumentType> persistedTypes = persistDocumentTypes(process.getDocumentTypes());
        process.setDocumentTypes(persistedTypes);
        return repository.save(process);
    }

    @Transactional
    public Boolean update(String processIdentifier, Process updatedProcess) {
        try {
            Optional<Process> optionalProcess = findByIdentifier(processIdentifier);
            if (!optionalProcess.isPresent()) {
                return false;
            }
            Process process = optionalProcess.get();
            List<DocumentType> documentTypes = updatedProcess.getDocumentTypes();
            if (documentTypes != null) {
                List<DocumentType> persistedTypes = persistDocumentTypes(documentTypes);
                process.setDocumentTypes(persistedTypes);
            }
            if (updatedProcess.getServiceCode() != null) {
                process.setServiceCode(updatedProcess.getServiceCode());
            }
            if (updatedProcess.getServiceEditionCode() != null) {
                process.setServiceEditionCode(updatedProcess.getServiceEditionCode());
            }
            Process updated = repository.save(process);
            return updated != null;
        } catch (Exception e) {
            throw new EntityNotFoundException(updatedProcess.getIdentifier());
        }
    }

    private List<DocumentType> persistDocumentTypes(List<DocumentType> documentTypes) {
        List<DocumentType> persistedTypes = Lists.newArrayList();
        for (DocumentType documentType : documentTypes) {
            Optional<DocumentType> type = documentTypeService.findByIdentifier(documentType.getIdentifier());
            if (!type.isPresent()) {
                persistedTypes.add(documentTypeService.add(documentType));
            } else {
                persistedTypes.add(type.get());
            }
        }
        return persistedTypes;
    }

    @Transactional
    public void delete(Process process) {
        repository.delete(process);
    }

    @Transactional(readOnly = true)
    public List<Process> findAll() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Process> findAll(ProcessCategory processCategory) {
        return repository.findAllByCategory(processCategory);
    }

    @Transactional(readOnly = true)
    public Process getDefaultArkivmeldingProcess() {
        return repository.findByIdentifier(props.getElma().getDefaultProcessIdentifier());
    }
}

package no.difi.meldingsutveksling.serviceregistry.service;

import no.difi.meldingsutveksling.serviceregistry.EntityNotFoundException;
import no.difi.meldingsutveksling.serviceregistry.model.DocumentType;
import no.difi.meldingsutveksling.serviceregistry.persistence.DocumentTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DocumentTypeService {

    private final DocumentTypeRepository repository;

    public DocumentTypeService(DocumentTypeRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public DocumentType findByIdentifier(String identifier) {
        return repository.findByIdentifier(identifier);
    }

    @Transactional
    public DocumentType add(DocumentType documentType) {
        return repository.save(documentType);
    }

    @Transactional
    public DocumentType update(DocumentType updatedDocumentType) {
        DocumentType existing = repository.findByIdentifier(updatedDocumentType.getIdentifier());
        if (null == existing) {
            throw new EntityNotFoundException(updatedDocumentType.getIdentifier());
        }
        if (updatedDocumentType.getProcesses() != null) {
            existing.setProcesses(updatedDocumentType.getProcesses());
        }
        return repository.save(existing);
    }

    @Transactional
    public void delete(DocumentType documentType) {
        repository.delete(documentType);
    }

    @Transactional(readOnly = true)
    public List<DocumentType> findAll() {
        return repository.findAll();
    }
}

package no.difi.meldingsutveksling.serviceregistry.service;

import no.difi.meldingsutveksling.serviceregistry.exceptions.EntityNotFoundException;
import no.difi.meldingsutveksling.serviceregistry.domain.BusinessMessageTypes;
import no.difi.meldingsutveksling.serviceregistry.domain.DocumentType;
import no.difi.meldingsutveksling.serviceregistry.persistence.DocumentTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class DocumentTypeService {

    private final DocumentTypeRepository repository;

    public DocumentTypeService(DocumentTypeRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Optional<DocumentType> findByIdentifier(String identifier) {
        return Optional.ofNullable(repository.findByIdentifier(identifier));
    }

    public Optional<DocumentType> findByBusinessMessageType(BusinessMessageTypes type) {
        return repository.findAll().stream()
                .filter(dt -> dt.getIdentifier().endsWith(type.toString().toLowerCase()))
                .findFirst();
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

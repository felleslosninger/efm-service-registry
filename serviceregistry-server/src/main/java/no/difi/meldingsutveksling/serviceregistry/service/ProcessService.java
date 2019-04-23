package no.difi.meldingsutveksling.serviceregistry.service;

import no.difi.meldingsutveksling.serviceregistry.EntityNotFoundException;
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

    public ProcessService(ProcessRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Optional<Process> findByIdentifier(String identifier) {
        return Optional.ofNullable(repository.findByIdentifier(identifier));
    }

    @Transactional
    public Process add(Process process) {
        return repository.save(process);
    }

    @Transactional
    public Process update(Process updatedProcess) {
        Process existing = repository.findByIdentifier(updatedProcess.getIdentifier());
        if (null == existing) {
            throw new EntityNotFoundException(updatedProcess.getIdentifier());
        }
        if (updatedProcess.getServiceCode() != null) {
            existing.setServiceCode(updatedProcess.getServiceCode());
        }
        if (updatedProcess.getServiceEditionCode() != null) {
            existing.setServiceEditionCode(updatedProcess.getServiceEditionCode());
        }
        if (updatedProcess.getDocumentTypes() != null) {
            existing.setDocumentTypes(updatedProcess.getDocumentTypes());
        }
        return repository.save(existing);
    }

    @Transactional
    public Boolean updateProcess(String processIdentifier, Process updatedProcess) {
        try {
            Optional<Process> optionalProcess = findByIdentifier(processIdentifier);
            if (!optionalProcess.isPresent()) {
                return false;
            }
            Process process = optionalProcess.get();
            if (updatedProcess.getDocumentTypes() != null) {
                process.setDocumentTypes(updatedProcess.getDocumentTypes());
            }
            if (updatedProcess.getServiceCode() != null) {
                process.setServiceCode(updatedProcess.getServiceCode());
            }
            if (updatedProcess.getServiceCode() != null) {
                process.setServiceEditionCode(updatedProcess.getServiceEditionCode());
            }
            return true;
        } catch (Exception e) {
            throw new EntityNotFoundException(updatedProcess.getIdentifier());
        }
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
        return repository.findAllByCategory();
    }
}

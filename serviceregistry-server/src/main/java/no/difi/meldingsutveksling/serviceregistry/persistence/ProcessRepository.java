package no.difi.meldingsutveksling.serviceregistry.persistence;

import no.difi.meldingsutveksling.serviceregistry.model.Process;
import no.difi.meldingsutveksling.serviceregistry.model.ProcessCategory;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProcessRepository extends CrudRepository<Process, Long> {

    Process findByIdentifier(String identifier);

    List<Process> findAll();

    List<Process> findAllByCategory(ProcessCategory processCategory);
}

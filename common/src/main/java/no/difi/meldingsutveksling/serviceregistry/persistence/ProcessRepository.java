package no.difi.meldingsutveksling.serviceregistry.persistence;

import no.difi.meldingsutveksling.serviceregistry.domain.Process;
import no.difi.meldingsutveksling.serviceregistry.domain.ProcessCategory;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface ProcessRepository extends CrudRepository<Process, Long> {

    Process findByIdentifier(String identifier);

    List<Process> findAll();

    Set<Process> findAllByCategory(ProcessCategory processCategory);
}

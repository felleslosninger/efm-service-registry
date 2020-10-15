package no.difi.meldingsutveksling.serviceregistry.persistence;

import no.difi.meldingsutveksling.serviceregistry.domain.DocumentType;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentTypeRepository extends CrudRepository<DocumentType, Long> {

    DocumentType findByIdentifier(String identifier);
    List<DocumentType> findAll();

}

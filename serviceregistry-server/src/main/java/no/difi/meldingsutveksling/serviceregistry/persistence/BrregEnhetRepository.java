package no.difi.meldingsutveksling.serviceregistry.persistence;


import no.difi.meldingsutveksling.serviceregistry.model.BrregEnhet;
import org.springframework.data.repository.CrudRepository;

public interface BrregEnhetRepository extends CrudRepository<BrregEnhet, String> {
}

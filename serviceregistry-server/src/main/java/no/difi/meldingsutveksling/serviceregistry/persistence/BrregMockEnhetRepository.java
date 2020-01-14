package no.difi.meldingsutveksling.serviceregistry.persistence;


import no.difi.meldingsutveksling.serviceregistry.model.BrregMockEnhet;
import org.springframework.data.repository.CrudRepository;

public interface BrregMockEnhetRepository extends CrudRepository<BrregMockEnhet, String> {
}

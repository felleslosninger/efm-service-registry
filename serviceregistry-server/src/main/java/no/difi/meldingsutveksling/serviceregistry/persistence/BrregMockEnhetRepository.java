package no.difi.meldingsutveksling.serviceregistry.persistence;


import no.difi.meldingsutveksling.serviceregistry.domain.BrregMockEnhet;
import org.springframework.data.repository.CrudRepository;

public interface BrregMockEnhetRepository extends CrudRepository<BrregMockEnhet, String> {
}

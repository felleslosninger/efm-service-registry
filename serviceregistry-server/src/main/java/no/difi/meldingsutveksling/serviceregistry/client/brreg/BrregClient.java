package no.difi.meldingsutveksling.serviceregistry.client.brreg;

import no.difi.meldingsutveksling.serviceregistry.domain.BrregEnhet;

import java.util.Optional;

public interface BrregClient {
    Optional<BrregEnhet> getBrregEnhetByOrgnr(String orgnr);
    Optional<BrregEnhet> getBrregUnderenhetByOrgnr(String orgnr);
}

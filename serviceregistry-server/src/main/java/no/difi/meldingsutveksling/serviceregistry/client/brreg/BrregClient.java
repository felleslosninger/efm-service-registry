package no.difi.meldingsutveksling.serviceregistry.client.brreg;

import no.difi.meldingsutveksling.domain.Iso6523;
import no.difi.meldingsutveksling.serviceregistry.domain.BrregEnhet;

import java.util.Optional;

public interface BrregClient {
    Optional<BrregEnhet> getBrregEnhetByOrgnr(Iso6523 orgnr);
    Optional<BrregEnhet> getBrregUnderenhetByOrgnr(Iso6523 orgnr);
}

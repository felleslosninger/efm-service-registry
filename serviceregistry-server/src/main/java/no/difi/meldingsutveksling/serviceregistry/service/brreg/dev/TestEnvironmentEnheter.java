package no.difi.meldingsutveksling.serviceregistry.service.brreg.dev;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.serviceregistry.domain.BrregMockEnhet;
import no.difi.meldingsutveksling.serviceregistry.persistence.BrregMockEnhetRepository;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class TestEnvironmentEnheter {

    private final BrregMockEnhetRepository brregMockRepo;

    /**
     * Add test users if they don't exist.
     */
    @PostConstruct
    private void init() {
        createBrregEnhet("Biristrand og Tjøtta", "ORGL", "910075918");
        createBrregEnhet("Lote og Årviksand", "ORGL", "910077473");
        createBrregEnhet("Stårheim og Røst", "ORGL", "910094092");
        createBrregEnhet("Røn og Ranheim", "ORGL", "810076402");
        createBrregEnhet("Dølemo og Ramberg", "ORGL", "910076787");
        createBrregEnhet("Østby og Sandøy", "ORGL", "910094548");
        createBrregEnhet("Reipå og Bugøynes", "ORGL", "910085379");
        createBrregEnhet("Norfold og Henningsvær", "AS", "910071696");
        createBrregEnhet("Aure og Darbu", "AS", "810074582");
        // NVE test users
        createBrregEnhet("Hasselvika og Stabbestad", "AS", "810196742");
        createBrregEnhet("Nord-Lenangen og Sørumsand", "AS", "910195611");
        createBrregEnhet("Vallersund og Sandset", "AS", "910175300");
        createBrregEnhet("Hesseng og Våle Revisjon", "AS", "910219308");
        // FIKS test user
        createBrregEnhet("Sømådalen og Bessaker Revisjon", "KOMM", "910229028");
        createBrregEnhet("FRISK VOKSENDE TIGER AS", "AS", "314244370");
        createBrregEnhet("HUND", "AS", "311780735");
        createBrregEnhet("KUL SLITEN TIGER AS", "AS", "314240979");
        createBrregEnhet("FILOSOFISK BEGEISTRET APE", "AS", "313711218");
    }

    private void createBrregEnhet(String navn, String organisasjonsform, String organisasjonsnummer) {
        addBrregEnhet(BrregMockEnhet.of(organisasjonsnummer, navn, organisasjonsform));
    }

    public boolean addBrregEnhet(BrregMockEnhet enhet) {
        Optional<BrregMockEnhet> existing = brregMockRepo.findById(enhet.getOrgnr());
        if (existing.isPresent()) return false;

        brregMockRepo.save(enhet);
        return true;
    }

    public Optional<BrregMockEnhet> getBrregEnhet(String orgnr) {
        return brregMockRepo.findById(orgnr);
    }

    public void deleteBrregEnhet(String orgnr) {
        brregMockRepo.deleteById(orgnr);
    }
}

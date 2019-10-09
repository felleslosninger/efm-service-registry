package no.difi.meldingsutveksling.serviceregistry.service.brreg.dev;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.serviceregistry.model.BrregEnhet;
import no.difi.meldingsutveksling.serviceregistry.model.BrregPostadresse;
import no.difi.meldingsutveksling.serviceregistry.persistence.BrregEnhetRepository;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class TestEnvironmentEnheter {

    private final BrregEnhetRepository brregMockRepo;

    /**
     * Add test users if they don't exist.
     */
    @PostConstruct
    private void init() {
        BrregPostadresse defaultPostadr = new BrregPostadresse("Postboks 1337", "1337", "OSLO", "Norge");
        createBrregEnhet("Biristrand og Tjøtta", "ORGL", "910075918");
        createBrregEnhet("Lote og Årviksand", "ORGL", "910077473");
        createBrregEnhet("Stårheim og Røst", "ORGL", "910094092");
        createBrregEnhet("Røn og Ranheim", "ORGL", "810076402");
        createBrregEnhet("Dølemo og Ramberg", "ORGL", "910076787", defaultPostadr);
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
    }

    private void createBrregEnhet(String navn, String organisasjonsform, String organisasjonsnummer) {
        BrregEnhet brregEnhet = new BrregEnhet();
        brregEnhet.setOrganisasjonsnummer(organisasjonsnummer);
        brregEnhet.setNavn(navn);
        brregEnhet.setOrganisasjonsform(organisasjonsform);
        addBrregEnhet(brregEnhet);
    }

    private void createBrregEnhet(String navn, String organisasjonsform, String organisasjonsnummer, BrregPostadresse adr) {
        BrregEnhet brregEnhet = new BrregEnhet();
        brregEnhet.setOrganisasjonsnummer(organisasjonsnummer);
        brregEnhet.setNavn(navn);
        brregEnhet.setOrganisasjonsform(organisasjonsform);
        brregEnhet.setPostadresse(adr);
        addBrregEnhet(brregEnhet);
    }

    public boolean addBrregEnhet(BrregEnhet enhet) {
        Optional<BrregEnhet> existing = brregMockRepo.findById(enhet.getOrganisasjonsnummer());
        if (existing.isPresent()) return false;

        brregMockRepo.save(enhet);
        return true;
    }

    public Optional<BrregEnhet> getBrregEnhet(String orgnr) {
        return brregMockRepo.findById(orgnr);
    }

    public void deleteBrregEnhet(String orgnr) {
        brregMockRepo.deleteById(orgnr);
    }
}

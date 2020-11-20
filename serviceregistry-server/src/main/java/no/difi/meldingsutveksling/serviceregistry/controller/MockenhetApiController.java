package no.difi.meldingsutveksling.serviceregistry.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.serviceregistry.domain.BrregMockEnhet;
import no.difi.meldingsutveksling.serviceregistry.service.brreg.dev.TestEnvironmentEnheter;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

@Profile({"dev", "staging"})
@RequestMapping("/api/v1/mockenhet")
@RestController
@Slf4j
@RequiredArgsConstructor
public class MockenhetApiController {

    private final TestEnvironmentEnheter testEnheter;

    @PostMapping()
    @ResponseBody
    public String addMockEnhet(@RequestBody BrregMockEnhet enhet) {
        log.info("Adding {} to Brreg mock", enhet);
        if (testEnheter.addBrregEnhet(enhet)) {
            return "Lagt til i mock";
        }
        return "Enheten eksisterer allerede";
    }

    @DeleteMapping(value = "/{orgnr}")
    public void delMockEnhet(@PathVariable String orgnr) {
        log.info("Deleting mock entry with identifier {}", orgnr);
        testEnheter.deleteBrregEnhet(orgnr);
    }
}

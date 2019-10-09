package no.difi.meldingsutveksling.serviceregistry.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.serviceregistry.model.BrregEnhet;
import no.difi.meldingsutveksling.serviceregistry.service.brreg.dev.TestEnvironmentEnheter;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
public class AddMockEnhetController {

    private final TestEnvironmentEnheter testEnheter;

    @PostMapping("/addmockenhet")
    @ResponseBody
    public String addMockEnhet(@RequestBody BrregEnhet enhet) {
        log.info("Adding {} to Brreg mock", enhet);
        if (testEnheter.addBrregEnhet(enhet)) {
            return "Lagt til i mock";
        }
        return "Enheten eksisterer allerede";
    }

    @GetMapping(value = "/deletemockenhet/{orgnr}")
    public void delMockEnhet(@PathVariable String orgnr) {
        log.info("Deleting mock entry with identifier {}", orgnr);
        testEnheter.deleteBrregEnhet(orgnr);
    }
}

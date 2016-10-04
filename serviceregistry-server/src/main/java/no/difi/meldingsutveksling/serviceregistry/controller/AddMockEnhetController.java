package no.difi.meldingsutveksling.serviceregistry.controller;

import no.difi.meldingsutveksling.serviceregistry.model.BrregEnhet;
import no.difi.meldingsutveksling.serviceregistry.service.brreg.dev.TestEnvironmentEnheter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
public class AddMockEnhetController {

    private static final Logger logger = LoggerFactory.getLogger(AddMockEnhetController.class);

    @Autowired
    TestEnvironmentEnheter testEnheter;

    @RequestMapping(method = RequestMethod.POST, value = "/addmockenhet")
    @ResponseBody
    public String addMockEnhet(@RequestBody BrregEnhet enhet) {
        logger.info("Adding {} to Brreg mock", enhet);
        if (testEnheter.addBrregEnhet(enhet)) {
            return "Lagt til i mock";
        }
        return "Enheten eksisterer allerede";
    }

    @RequestMapping(value = "/deletemockenhet/{orgnr}")
    public void delMockEnhet(@PathVariable String orgnr) {
        logger.info("Deleting mock entry with identifier {}", orgnr);
        testEnheter.deleteBrregEnhet(orgnr);
    }
}

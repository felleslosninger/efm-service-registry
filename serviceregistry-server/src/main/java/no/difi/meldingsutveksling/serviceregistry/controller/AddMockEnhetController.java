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
    public BrregEnhet addMockEnhet(@RequestBody BrregEnhet enhet) {
        logger.info("Adding {} to Brreg mock", enhet);
        testEnheter.addBrregEnhet(enhet);
        return enhet;
    }

    @RequestMapping(value = "/deletemockenhet/{orgnr}")
    public void delMockEnhet(@PathVariable String orgnr) {
        logger.info("Deleting mock entry with identifier {}", orgnr);
        testEnheter.deleteBrregEnhet(orgnr);
    }
}

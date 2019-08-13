package no.difi.meldingsutveksling.serviceregistry.controller;


import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MockEnhetController {

    @GetMapping("/mockenhet")
    public String mockEnhet() {
        return "mockenhet";
    }

}

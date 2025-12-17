package no.difi.meldingsutveksling.serviceregistry.controller;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class JwkController {

    private final String jwkJson;

    public JwkController(RSAKey rsaKey) {
        this.jwkJson = new JWKSet(rsaKey).toString();
    }

    @GetMapping(value = "/jwk", produces = MediaType.APPLICATION_JSON_VALUE)
    public String jwkEndpoint() {
        return jwkJson;
    }
}


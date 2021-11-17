package no.difi.meldingsutveksling.serviceregistry.controller

import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.RSAKey
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class JwkController(rsaKey: RSAKey) {
    private val jwkJson: String = JWKSet(rsaKey).toString(true)

    @GetMapping(value = ["/jwk"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun jwkEndpoint(): String {
        return jwkJson
    }
}
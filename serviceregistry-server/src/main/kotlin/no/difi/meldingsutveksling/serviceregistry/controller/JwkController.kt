package no.difi.meldingsutveksling.serviceregistry.controller

import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.RSAKey
import no.difi.meldingsutveksling.serviceregistry.logger
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class JwkController(rsaKey: RSAKey) {
    val log = logger()
    private val jwkJson: String = JWKSet(rsaKey).toJSONObject().toJSONString()

    @GetMapping("/jwk")
    fun jwkEndpoint(): String {
        return jwkJson
    }
}
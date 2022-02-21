package no.difi.meldingsutveksling.serviceregistry.controller

import no.difi.meldingsutveksling.serviceregistry.domain.ServiceIdentifier.DPE
import no.difi.meldingsutveksling.serviceregistry.domain.ServiceIdentifier.DPO
import no.difi.meldingsutveksling.serviceregistry.exceptions.NonMatchingCertificatesException
import no.difi.meldingsutveksling.serviceregistry.security.PayloadSigner
import no.difi.meldingsutveksling.serviceregistry.service.AuthenticationService
import no.difi.meldingsutveksling.serviceregistry.service.virksert.VirkSertService
import org.springframework.http.MediaType
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/virksert")
class VirksertController(
    private val virkSertService: VirkSertService,
    private val payloadSigner: PayloadSigner,
    private val authenticationService: AuthenticationService
) {

    @GetMapping("/{identifier}", produces = [MediaType.TEXT_PLAIN_VALUE])
    fun getCertificate(@PathVariable identifier: String, auth: Authentication): String {
        return with(authenticationService.getToken(auth).claims["scope"] as String) {
            when {
                contains("move/dpo.read") && contains("move/dpe.read") -> {
                    val dpoCert = virkSertService.getCertificate(identifier, DPO)
                    val dpeCert = virkSertService.getCertificate(identifier, DPE)
                    if (dpoCert != dpeCert) {
                        throw NonMatchingCertificatesException(identifier)
                    }
                    return@with dpoCert
                }
                contains("move/dpo.read") -> return@with virkSertService.getCertificate(identifier, DPO)
                contains("move/dpe.read") -> return@with virkSertService.getCertificate(identifier, DPE)
                else -> return@with virkSertService.getCertificate(identifier, DPO)
            }
        }
    }

    @GetMapping("/{identifier}", produces = ["application/jose"])
    fun getCertificateJose(@PathVariable identifier: String, auth: Authentication): String {
        return payloadSigner.sign(getCertificate(identifier, auth))
    }

}
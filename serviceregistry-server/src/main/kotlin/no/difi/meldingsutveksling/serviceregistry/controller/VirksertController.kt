package no.difi.meldingsutveksling.serviceregistry.controller

import no.difi.meldingsutveksling.serviceregistry.domain.ServiceIdentifier
import no.difi.meldingsutveksling.serviceregistry.domain.ServiceIdentifier.DPO
import no.difi.meldingsutveksling.serviceregistry.security.PayloadSigner
import no.difi.meldingsutveksling.serviceregistry.service.virksert.VirkSertService
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/virksert")
class VirksertController(
    private val virkSertService: VirkSertService,
    private val payloadSigner: PayloadSigner
) {

    @GetMapping("/{identifier}", produces = [MediaType.TEXT_PLAIN_VALUE])
    fun getCertificate(@PathVariable identifier: String): String {
        return virkSertService.getCertificate(identifier, DPO)
    }

    @GetMapping("/{identifier}", produces = ["application/jose"])
    fun getCertificateJose(@PathVariable identifier: String): String {
        return payloadSigner.sign(getCertificate(identifier))
    }

    @GetMapping("/{identifier}/service/{serviceIdentifier}", produces = [MediaType.TEXT_PLAIN_VALUE])
    fun getCertificate(@PathVariable identifier: String,
                       @PathVariable serviceIdentifier: ServiceIdentifier): String {
        return virkSertService.getCertificate(identifier, serviceIdentifier)
    }

    @GetMapping("/{identifier}/service/{serviceIdentifier}", produces = ["application/jose"])
    fun getCertificateJose(@PathVariable identifier: String,
                           @PathVariable serviceIdentifier: ServiceIdentifier): String {
        return payloadSigner.sign(getCertificate(identifier, serviceIdentifier))
    }

}
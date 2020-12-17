package no.difi.meldingsutveksling.serviceregistry.controller

import no.difi.meldingsutveksling.serviceregistry.service.virksert.VirkSertService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/virksert")
class VirksertController(private val virkSertService: VirkSertService) {

    @GetMapping("/{identifier}")
    fun getCertificate(@PathVariable identifier: String): String {
        return virkSertService.getCertificate(identifier)
    }

}
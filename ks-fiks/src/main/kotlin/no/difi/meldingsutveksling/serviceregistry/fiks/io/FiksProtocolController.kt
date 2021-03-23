package no.difi.meldingsutveksling.serviceregistry.fiks.io

import no.difi.meldingsutveksling.serviceregistry.exceptions.ProcessNotFoundException
import no.difi.meldingsutveksling.serviceregistry.service.ProcessService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import javax.transaction.Transactional

@RestController
@RequestMapping("/api/v1/fiks")
open class FiksProtocolController(
    val processService: ProcessService,
    private val fiksProtocolRepository: FiksProtocolRepository
) {

    @PostMapping("{/identifier}")
    @Transactional
    @Throws(ProcessNotFoundException::class)
    open fun addProtocol(@PathVariable identifier: String): ResponseEntity<*> {
        val p = fiksProtocolRepository.findByIdentifier(identifier)
            ?: fiksProtocolRepository.save(FiksProtocol(identifier = identifier))

        return ResponseEntity.ok(p)
    }

    @DeleteMapping("/{identifier}")
    @Transactional
    open fun delProtocol(@PathVariable identifier: String): ResponseEntity<*> {
        fiksProtocolRepository.findByIdentifier(identifier) ?: return ResponseEntity.notFound().build<Any>()
        fiksProtocolRepository.deleteByIdentifier(identifier)
        return ResponseEntity.ok().build<Any>()
    }

    @GetMapping
    open fun listProtocols(): ResponseEntity<*> {
        return ResponseEntity.ok(fiksProtocolRepository.findAll())
    }

    @GetMapping("/{identifier}")
    open fun findby(@PathVariable identifier: String): ResponseEntity<*> {
        val find = fiksProtocolRepository.findByIdentifier(identifier)
        return find?.let { ResponseEntity.ok(it) } ?: ResponseEntity.notFound().build<Any>()
    }

}
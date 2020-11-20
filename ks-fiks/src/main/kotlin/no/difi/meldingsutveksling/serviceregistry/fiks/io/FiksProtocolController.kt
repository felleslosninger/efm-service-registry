package no.difi.meldingsutveksling.serviceregistry.fiks.io

import no.difi.meldingsutveksling.serviceregistry.exceptions.ProcessNotFoundException
import no.difi.meldingsutveksling.serviceregistry.service.ProcessService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import javax.transaction.Transactional

@RestController
@RequestMapping("/api/v1/fiks")
open class FiksProtocolController(val processService: ProcessService,
                                  private val fiksProtocolRepository: FiksProtocolRepository) {

    data class FiksProtocolRequestBody(var protocol: String?, var efmProcesses: Set<String>?)

    @PostMapping
    @Transactional
    @Throws(ProcessNotFoundException::class)
    open fun addProtocol(@RequestBody request: FiksProtocolRequestBody): ResponseEntity<*> {
        val protocol: String = request.protocol ?: return ResponseEntity.badRequest().body("protocol cannot be empty")
        val efmProcesses: Set<String> = request.efmProcesses?.toSet()
                ?: return ResponseEntity.badRequest().body("efmProcesses cannot be empty")

        val proto = efmProcesses.map {
            processService.findByIdentifier(it).orElseThrow { throw ProcessNotFoundException(it) }
        }.toMutableSet().let { proc ->
            fiksProtocolRepository.findByIdentifier(protocol)?.let { p ->
                p.processes = proc
                fiksProtocolRepository.save(p)
            } ?: fiksProtocolRepository.save(FiksProtocol(identifier = protocol, processes = proc))
        }

        return ResponseEntity.ok(proto)
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
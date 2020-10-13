package no.difi.meldingsutveksling.serviceregistry.fiks.io

import no.difi.meldingsutveksling.serviceregistry.CacheConfig
import no.difi.meldingsutveksling.serviceregistry.SRRequestScope
import no.difi.meldingsutveksling.serviceregistry.config.ServiceregistryProperties
import no.difi.meldingsutveksling.serviceregistry.domain.EntityInfo
import no.difi.meldingsutveksling.serviceregistry.domain.Process
import no.difi.meldingsutveksling.serviceregistry.logger
import no.difi.meldingsutveksling.serviceregistry.logging.SRMarkerFactory.markerFrom
import no.ks.fiks.fiksio.client.api.katalog.model.KatalogKonto
import no.ks.fiks.io.client.model.Konto
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.cache.annotation.Cacheable
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.util.UriBuilder
import reactor.core.publisher.Mono
import java.net.URI
import java.time.Duration
import java.util.*

@Component
open class FiksIoService(private val props: ServiceregistryProperties,
                         private val fiksProtocolRepository: FiksProtocolRepository,
                         private val requestScope: SRRequestScope) {
    val log = logger()

    private val wc = WebClient.builder()
            .baseUrl(props.fiks.io.endpointUrl)
            .defaultHeaders {
                it.set("IntegrasjonId", props.fiks.io.integrasjonId)
                it.set("IntegrasjonPassord", props.fiks.io.integrasjonPassord)
            }
            .build()

    @Cacheable(CacheConfig.FIKSIO_CACHE)
    open fun lookup(entity: EntityInfo, process: Process, securityLevel: Int): Optional<Konto> {
        if (!props.fiks.io.orgFormFilter.contains(entity.entityType.name)) return Optional.empty()

        val fiksProtocol = fiksProtocolRepository.findByProcessesIdentifier(process.identifier)
                ?: return Optional.empty()

        val uriBuilder: (UriBuilder) -> URI = {
            it.path("/fiks-io/katalog/api/v1/lookup")
                    .queryParam("identifikator", "ORG_NO.${entity.identifier}")
                    .queryParam("meldingProtokoll", fiksProtocol.identifier)
                    .queryParam("sikkerhetsniva", securityLevel)
                    .build()
        }
        return wc.get()
                .uri(uriBuilder)
                .header("Authorization", "Bearer ${requestScope.token}")
                .exchange()
                .flatMap { r ->
                    when {
                        r.statusCode() == HttpStatus.NOT_FOUND -> {
                            Mono.empty<KatalogKonto>() }
                        r.statusCode().is4xxClientError -> {
                            r.createException().flatMap {
                                log.warn(markerFrom(requestScope), "Client error ${it.statusCode} when looking up ${it.request?.uri} - ${it.responseBodyAsString}")
                                Mono.empty<KatalogKonto>()
                            }
                        }
                        r.statusCode().isError -> {
                            r.createException().flatMap {
                                log.error(markerFrom(requestScope), "Server error when looking up ${it.request?.uri}")
                                Mono.error<KatalogKonto>(it)
                            }
                        }
                        else -> {
                            r.bodyToMono(KatalogKonto::class.java)
                        }
                    }
                }
                .block(Duration.ofSeconds(3))
                ?.let { Optional.of(Konto.fromKatalogModel(it)) } ?: Optional.empty()
    }

}

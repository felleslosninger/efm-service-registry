package no.difi.meldingsutveksling.serviceregistry.fiks.io

import no.difi.meldingsutveksling.serviceregistry.CacheConfig
import no.difi.meldingsutveksling.serviceregistry.SRRequestScope
import no.difi.meldingsutveksling.serviceregistry.config.ServiceregistryProperties
import no.difi.meldingsutveksling.serviceregistry.domain.EntityInfo
import no.difi.meldingsutveksling.serviceregistry.exceptions.EntityNotFoundException
import no.difi.meldingsutveksling.serviceregistry.exceptions.ServiceRegistryException
import no.difi.meldingsutveksling.serviceregistry.logger
import no.ks.fiks.fiksio.client.api.katalog.model.KatalogKonto
import no.ks.fiks.io.client.model.Konto
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.cache.annotation.Cacheable
import org.springframework.http.HttpStatus
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.util.UriBuilder
import reactor.core.publisher.Mono
import java.net.URI
import java.time.Duration
import java.util.*

@Component
@ConditionalOnProperty(name = ["difi.move.fiks.io.enable"], havingValue = "true")
open class FiksIoService(
    private val props: ServiceregistryProperties,
    private val requestScope: SRRequestScope
) {
    val log = logger()

    private val wc = WebClient.builder()
        .baseUrl(props.fiks.io.endpointUrl)
        .defaultHeaders {
            it.set("IntegrasjonId", props.fiks.io.integrasjonId)
            it.set("IntegrasjonPassord", props.fiks.io.integrasjonPassord)
        }
        .build()

    @Cacheable(CacheConfig.FIKSIO_CACHE)
    open fun lookup(identifier: String): Optional<Konto> {
        return wc.get()
            .uri("/fiks-io/katalog/api/v1/kontoer/${identifier}")
            .header("Authorization", "Bearer ${requestScope.token.tokenValue}")
            .retrieve()
            .onStatus(HttpStatus::isError) { r ->
                when (r.statusCode()) {
                    HttpStatus.NOT_FOUND -> Mono.error(EntityNotFoundException(identifier))
                    HttpStatus.UNAUTHORIZED -> r.createException()
                        .flatMap { Mono.error(AccessDeniedException(it.message, it)) }
                    else -> r.createException()
                        .flatMap { Mono.error(ServiceRegistryException(it)) }
                }
            }
            .bodyToMono(KatalogKonto::class.java)
            .block(Duration.ofSeconds(5))
            ?.let { Optional.of(Konto.fromKatalogModel(it)) } ?: Optional.empty()
    }

    @Cacheable(CacheConfig.FIKSIO_CACHE)
    open fun lookup(entity: EntityInfo, protocol: String, securityLevel: Int): Optional<Konto> {
        val uriBuilder: (UriBuilder) -> URI = {
            it.path("/fiks-io/katalog/api/v1/lookup")
                .queryParam("identifikator", "ORG_NO.${entity.identifier}")
                .queryParam("meldingProtokoll", protocol)
                .queryParam("sikkerhetsniva", securityLevel)
                .build()
        }
        return wc.get()
            .uri(uriBuilder)
            .header("Authorization", "Bearer ${requestScope.token.tokenValue}")
            .exchange()
            .flatMap { r ->
                when {
                    r.statusCode() == HttpStatus.NOT_FOUND -> {
                        Mono.empty<KatalogKonto>()
                    }
                    r.statusCode().is4xxClientError -> {
                        r.createException().flatMap {
                            log.warn("Client error ${it.statusCode} when looking up ${it.request?.uri} - ${it.responseBodyAsString}")
                            Mono.empty<KatalogKonto>()
                        }
                    }
                    r.statusCode().isError -> {
                        r.createException().flatMap {
                            log.error("Server error when looking up ${it.request?.uri}")
                            Mono.error<KatalogKonto>(it)
                        }
                    }
                    else -> {
                        r.bodyToMono(KatalogKonto::class.java)
                    }
                }
            }
            .block(Duration.ofSeconds(5))
            ?.let { Optional.of(Konto.fromKatalogModel(it)) } ?: Optional.empty()
    }

}

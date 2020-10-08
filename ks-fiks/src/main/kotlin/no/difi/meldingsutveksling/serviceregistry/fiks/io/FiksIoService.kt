package no.difi.meldingsutveksling.serviceregistry.fiks.io

import no.difi.meldingsutveksling.serviceregistry.CacheConfig
import no.difi.meldingsutveksling.serviceregistry.SRRequestScope
import no.difi.meldingsutveksling.serviceregistry.config.ServiceregistryProperties
import no.difi.meldingsutveksling.serviceregistry.domain.EntityInfo
import no.difi.meldingsutveksling.serviceregistry.domain.Process
import no.difi.meldingsutveksling.serviceregistry.logger
import no.ks.fiks.fiksio.client.api.katalog.model.KatalogKonto
import no.ks.fiks.io.client.model.Konto
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.cache.annotation.Cacheable
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.time.Duration
import java.util.*

@Component
open class FiksIoService(private val props: ServiceregistryProperties,
                         private val fiksProtocolRepository: FiksProtocolRepository,
                         private val requestScope: SRRequestScope) {
    val log = logger()

    private val rt: RestTemplate = RestTemplateBuilder()
            .rootUri(props.fiks.io.endpointUrl)
            .setConnectTimeout(Duration.ofSeconds(3))
            .setReadTimeout(Duration.ofSeconds(3))
            .build()

    @Cacheable(CacheConfig.FIKSIO_CACHE)
    open fun lookup(entity: EntityInfo, process: Process, securityLevel: Int): Optional<Konto> {
        if (!props.fiks.io.orgFormFilter.contains(entity.entityType.name)) return Optional.empty()

        val fiksProtocol = fiksProtocolRepository.findByProcessesIdentifier(process.identifier)
                ?: return Optional.empty()

        val headers = HttpHeaders()
        headers.add("IntegrasjonId", props.fiks.io.integrasjonId)
        headers.add("IntegrasjonPassord", props.fiks.io.integrasjonPassord)
        headers.add("Authorization", "Bearer ${requestScope.token}")
        val httpEntity = HttpEntity<Any>(headers)

        val uri = UriComponentsBuilder.fromUriString("/fiks-io/katalog/api/v1/lookup")
                .queryParam("identifikator", "ORG_NO.${entity.identifier}")
                .queryParam("meldingProtokoll", fiksProtocol.identifier)
                .queryParam("sikkerhetsniva", securityLevel)
                .build().toUriString()

        try {
            val res = rt.exchange(uri, HttpMethod.GET, httpEntity, KatalogKonto::class.java)
            return res.body?.let {
                Optional.ofNullable(Konto.fromKatalogModel(it))
            } ?: Optional.empty()
        } catch (e: HttpClientErrorException) {
            val errorMsg = "Error looking up ${entity.identifier} in Fiks Kontokatalog"
            when  {
                e.statusCode.is4xxClientError -> log.debug(errorMsg, e)
                else -> log.error(errorMsg, e)
            }
        }
        return Optional.empty()
    }

}

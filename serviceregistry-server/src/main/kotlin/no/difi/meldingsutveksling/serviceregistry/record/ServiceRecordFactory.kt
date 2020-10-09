package no.difi.meldingsutveksling.serviceregistry.record

import com.google.common.base.Strings
import no.difi.meldingsutveksling.serviceregistry.*
import no.difi.meldingsutveksling.serviceregistry.config.ServiceregistryProperties
import no.difi.meldingsutveksling.serviceregistry.domain.*
import no.difi.meldingsutveksling.serviceregistry.domain.ProcessCategory.ARKIVMELDING
import no.difi.meldingsutveksling.serviceregistry.domain.ProcessCategory.AVTALT
import no.difi.meldingsutveksling.serviceregistry.fiks.io.FiksProtocolRepository
import no.difi.meldingsutveksling.serviceregistry.krr.DsfLookupException
import no.difi.meldingsutveksling.serviceregistry.krr.LookupParameters
import no.difi.meldingsutveksling.serviceregistry.krr.PersonResource
import no.difi.meldingsutveksling.serviceregistry.krr.PostAddress
import no.difi.meldingsutveksling.serviceregistry.logging.SRMarkerFactory
import no.difi.meldingsutveksling.serviceregistry.service.DocumentTypeService
import no.difi.meldingsutveksling.serviceregistry.service.EntityService
import no.difi.meldingsutveksling.serviceregistry.service.ProcessService
import no.difi.meldingsutveksling.serviceregistry.service.brreg.BrregNotFoundException
import no.difi.meldingsutveksling.serviceregistry.service.krr.KrrService
import no.difi.meldingsutveksling.serviceregistry.service.virksert.VirkSertService
import no.difi.virksert.client.lang.VirksertClientException
import no.ks.fiks.io.client.model.Konto
import org.apache.commons.io.IOUtils
import org.springframework.stereotype.Component
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.*

@Component
class ServiceRecordFactory(private val fiksProtocolRepository: FiksProtocolRepository,
                           private val properties: ServiceregistryProperties,
                           private val virkSertService: VirkSertService,
                           private val processService: ProcessService,
                           private val documentTypeService: DocumentTypeService,
                           private val krrService: KrrService,
                           private val entityService: EntityService,
                           private val requestScope: SRRequestScope) {

    val log = logger()

    @Throws(CertificateNotFoundException::class)
    fun createDpoServiceRecord(orgnr: String, process: Process): ServiceRecord {
        val serviceRecord = when (process.category) {
            ARKIVMELDING -> ArkivmeldingServiceRecord.of(ServiceIdentifier.DPO, orgnr, properties.dpo.endpointURL.toString(), lookupPemCertificate(orgnr))
            AVTALT -> ArkivmeldingServiceRecord.of(ServiceIdentifier.DPO, orgnr, properties.dpo.endpointURL.toString(), lookupPemCertificate(orgnr))
            else -> throw IllegalArgumentException("Category ${process.category} not valid for DPO service record")
        }

        serviceRecord.process = process.identifier
        serviceRecord.service.serviceCode = properties.dpo.serviceCode
        serviceRecord.service.serviceEditionCode = properties.dpo.serviceEditionCode
        serviceRecord.documentTypes = process.documentTypes.map { it.identifier }.toList()
        return serviceRecord
    }

    fun createDpfServiceRecord(orgnr: String, process: Process, securityLevel: Int): ServiceRecord {
        val pem = try {
            IOUtils.toString(properties.fiks.svarut.certificate.inputStream, StandardCharsets.UTF_8)
        } catch (e: IOException) {
            log.error(SRMarkerFactory.markerFrom(requestScope), "Could not read certificate from {}", properties.fiks.svarut.certificate.toString())
            throw ServiceRegistryException(e)
        }
        val arkivmeldingServiceRecord: ServiceRecord = ArkivmeldingServiceRecord.of(ServiceIdentifier.DPF, orgnr, properties.fiks.svarut.serviceRecordUrl.toString(), pem)
        arkivmeldingServiceRecord.process = process.identifier
        arkivmeldingServiceRecord.documentTypes = process.documentTypes.map { it.identifier }.toList()
        arkivmeldingServiceRecord.service.securityLevel = securityLevel
        return arkivmeldingServiceRecord
    }

    fun createDigitalServiceRecord(personResource: PersonResource, identifier: String, p: Process): ServiceRecord? {
        val serviceRecord = SikkerDigitalPostServiceRecord(false, properties, personResource, ServiceIdentifier.DPI,
                identifier, null, null)
        serviceRecord.process = p.identifier
        documentTypeService.findByBusinessMessageType(BusinessMessageTypes.DIGITAL)
                .orElseThrow { missingDocTypeException(BusinessMessageTypes.DIGITAL) }
                .let { serviceRecord.documentTypes = listOf(it.identifier) }
        return serviceRecord
    }

    fun createDigitalDpvServiceRecord(identifier: String, process: Process): ServiceRecord {
        val dpvServiceRecord = ArkivmeldingServiceRecord.of(ServiceIdentifier.DPV, identifier, properties.dpv.endpointURL.toString())
        val defaultArkivmeldingProcess = processService.defaultArkivmeldingProcess
        dpvServiceRecord.service.serviceCode = defaultArkivmeldingProcess.serviceCode
        dpvServiceRecord.service.serviceEditionCode = defaultArkivmeldingProcess.serviceEditionCode
        dpvServiceRecord.process = process.identifier
        documentTypeService.findByBusinessMessageType(BusinessMessageTypes.DIGITAL_DPV)
                .orElseThrow { missingDocTypeException(BusinessMessageTypes.DIGITAL_DPV) }
                .let { dpvServiceRecord.documentTypes = listOf(it.identifier) }
        return dpvServiceRecord
    }

    @Throws(DsfLookupException::class, BrregNotFoundException::class)
    fun createPrintServiceRecord(identifier: String,
                                 onBehalfOrgnr: String,
                                 token: String,
                                 personResource: PersonResource,
                                 p: Process): Optional<ServiceRecord> {
        krrService.setPrintDetails(personResource)
        val dsfResource = krrService.getDSFInfo(LookupParameters.lookup(identifier).token(token))
                .orElseThrow { DsfLookupException("Receiver found in KRR on behalf of '$onBehalfOrgnr', but not in DSF.") }
        if (Strings.isNullOrEmpty(dsfResource.postAddress)) {
            // Some receivers have secret address - skip
            return Optional.empty()
        }
        val codeArea = dsfResource.postAddress.split(" ")
        val postAddress = PostAddress(dsfResource.name,
                dsfResource.street,
                codeArea[0],
                if (codeArea.size > 1) codeArea[1] else codeArea[0],
                dsfResource.country)
        val senderEntity: Optional<EntityInfo> = entityService.getEntityInfo(onBehalfOrgnr)
        val returnAddress = if (senderEntity.isPresent && senderEntity.get() is OrganizationInfo) {
            val orginfo = senderEntity.get() as OrganizationInfo
            PostAddress(orginfo.organizationName,
                    orginfo.postadresse.adresse,
                    orginfo.postadresse.postnummer,
                    orginfo.postadresse.poststed,
                    orginfo.postadresse.land)
        } else {
            throw BrregNotFoundException(String.format("Sender with identifier=%s not found in BRREG", onBehalfOrgnr))
        }
        val dpiServiceRecord = SikkerDigitalPostServiceRecord(true, properties, personResource, ServiceIdentifier.DPI,
                identifier, postAddress, returnAddress)
        dpiServiceRecord.process = p.identifier
        documentTypeService.findByBusinessMessageType(BusinessMessageTypes.PRINT)
                .orElseThrow { missingDocTypeException(BusinessMessageTypes.PRINT) }
                .let { dpiServiceRecord.documentTypes = listOf(it.identifier) }
        return Optional.of(dpiServiceRecord)
    }

    // TODO handle protocol not found
    fun createDpfioServiceRecord(orgnr: String, process: Process, konto: Konto): ServiceRecord {
        val protocol = fiksProtocolRepository.findByProcessesIdentifier(process.identifier)
                ?: throw FiksProtocolNotFoundException(process.identifier)
        return DpfioServiceRecord.from(orgnr, konto, process.identifier, protocol.identifier,
                process.documentTypes.map { it.identifier }.toList())
    }

    fun createDpvServiceRecord(orgnr: String, process: Process): ArkivmeldingServiceRecord {
        val dpvServiceRecord = ArkivmeldingServiceRecord.of(ServiceIdentifier.DPV, orgnr, properties.dpv.endpointURL.toString())
        dpvServiceRecord.service.serviceCode = process.serviceCode
        dpvServiceRecord.service.serviceEditionCode = process.serviceEditionCode
        dpvServiceRecord.process = process.identifier
        dpvServiceRecord.documentTypes = process.documentTypes.map { it.identifier }.toList()
        return dpvServiceRecord
    }

    @Throws(CertificateNotFoundException::class)
    fun createDpeServiceRecord(orgnr: String, process: Process): ServiceRecord {
        val pemCertificate: String = lookupPemCertificate(orgnr)
        val einnsynServiceRecord: ServiceRecord = DpeServiceRecord.of(pemCertificate, orgnr, ServiceIdentifier.DPE, process.serviceCode)
        einnsynServiceRecord.process = process.identifier
        einnsynServiceRecord.documentTypes = process.documentTypes.map { it.identifier }.toList()
        return einnsynServiceRecord
    }

    private fun lookupPemCertificate(orgnr: String): String {
        try {
            return virkSertService.getCertificate(orgnr)
        } catch (e: VirksertClientException) {
            throw CertificateNotFoundException("Unable to find certificate for: $orgnr", e)
        }
    }

    private fun missingDocTypeException(messageType: BusinessMessageTypes): RuntimeException {
        return RuntimeException("Missing DocumentType for business message type '$messageType'")
    }

}
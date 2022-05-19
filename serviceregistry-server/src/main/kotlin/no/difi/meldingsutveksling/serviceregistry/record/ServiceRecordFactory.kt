package no.difi.meldingsutveksling.serviceregistry.record

import com.google.common.base.Strings
import no.difi.meldingsutveksling.domain.Iso6523
import no.difi.meldingsutveksling.domain.PersonIdentifier
import no.difi.meldingsutveksling.serviceregistry.CertificateNotFoundException
import no.difi.meldingsutveksling.serviceregistry.SRRequestScope
import no.difi.meldingsutveksling.serviceregistry.config.ServiceregistryProperties
import no.difi.meldingsutveksling.serviceregistry.domain.*
import no.difi.meldingsutveksling.serviceregistry.domain.ServiceIdentifier.DPE
import no.difi.meldingsutveksling.serviceregistry.domain.ServiceIdentifier.DPO
import no.difi.meldingsutveksling.serviceregistry.exceptions.ServiceRegistryException
import no.difi.meldingsutveksling.serviceregistry.krr.KontaktInfoException
import no.difi.meldingsutveksling.serviceregistry.krr.LookupParameters
import no.difi.meldingsutveksling.serviceregistry.krr.PersonResource
import no.difi.meldingsutveksling.serviceregistry.krr.PostAddress
import no.difi.meldingsutveksling.serviceregistry.logger
import no.difi.meldingsutveksling.serviceregistry.logging.SRMarkerFactory
import no.difi.meldingsutveksling.serviceregistry.service.DocumentTypeService
import no.difi.meldingsutveksling.serviceregistry.service.EntityService
import no.difi.meldingsutveksling.serviceregistry.service.ProcessService
import no.difi.meldingsutveksling.serviceregistry.service.brreg.BrregNotFoundException
import no.difi.meldingsutveksling.serviceregistry.service.krr.KontaktInfoService
import no.difi.meldingsutveksling.serviceregistry.service.virksert.VirkSertService
import org.apache.commons.io.IOUtils
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Component
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.*

@Component
class ServiceRecordFactory(private val properties: ServiceregistryProperties,
                           private val virkSertService: VirkSertService,
                           private val processService: ProcessService,
                           private val documentTypeService: DocumentTypeService,
                           private val kontaktInfoService: KontaktInfoService,
                           private val entityService: EntityService,
                           private val requestScope: SRRequestScope) {

    val log = logger()

    @Throws(CertificateNotFoundException::class)
    fun createDpoServiceRecord(identifier: Iso6523, process: Process): ServiceRecord {
        val serviceRecord = ServiceRecord(DPO, identifier.orgIdentifier(), process, properties.dpo.endpointURL.toString())
        serviceRecord.pemCertificate = lookupPemCertificate(identifier, DPO)
        serviceRecord.service.serviceCode = properties.dpo.serviceCode
        serviceRecord.service.serviceEditionCode = properties.dpo.serviceEditionCode
        return serviceRecord
    }

    fun createDpfServiceRecord(identifier: Iso6523, process: Process, securityLevel: Int): ServiceRecord {
        val serviceRecord = ServiceRecord(ServiceIdentifier.DPF, identifier.orgIdentifier(), process, properties.fiks.svarut.serviceRecordUrl.toString())
        serviceRecord.pemCertificate = try {
            IOUtils.toString(properties.fiks.svarut.certificate.inputStream, StandardCharsets.UTF_8)
        } catch (e: IOException) {
            log.error(SRMarkerFactory.markerFrom(requestScope), "Could not read certificate from {}", properties.fiks.svarut.certificate.toString())
            throw ServiceRegistryException(e)
        }
        serviceRecord.service.securityLevel = securityLevel
        return serviceRecord
    }

    fun createDigitalServiceRecord(personResource: PersonResource, identifier: PersonIdentifier, process: Process): ServiceRecord {
        val serviceRecord = SikkerDigitalPostServiceRecord(identifier, process, personResource, properties.dpi.endpointURL.toString(), false, null, null)
        documentTypeService.findByBusinessMessageType(BusinessMessageTypes.DIGITAL)
                .orElseThrow { missingDocTypeException(BusinessMessageTypes.DIGITAL) }
                .let { serviceRecord.documentTypes = listOf(it.identifier) }
        return serviceRecord
    }

    fun createDigitalDpvServiceRecord(identifier: PersonIdentifier, process: Process): ServiceRecord {
        val dpvServiceRecord = ServiceRecord(ServiceIdentifier.DPV, identifier.identifier, process, properties.dpv.endpointURL.toString())
        val defaultArkivmeldingProcess = processService.defaultArkivmeldingProcess
        dpvServiceRecord.service.serviceCode = defaultArkivmeldingProcess.serviceCode
        dpvServiceRecord.service.serviceEditionCode = defaultArkivmeldingProcess.serviceEditionCode
        documentTypeService.findByBusinessMessageType(BusinessMessageTypes.DIGITAL_DPV)
                .orElseThrow { missingDocTypeException(BusinessMessageTypes.DIGITAL_DPV) }
                .let { dpvServiceRecord.documentTypes = listOf(it.identifier) }
        return dpvServiceRecord
    }

    @Throws(KontaktInfoException::class, BrregNotFoundException::class)
    fun createPrintServiceRecord(identifier: PersonIdentifier,
                                 clientIdentifier: Iso6523,
                                 token: Jwt,
                                 personResource: PersonResource,
                                 p: Process,
                                 print: Boolean): Optional<ServiceRecord> {
        if(!print) {
            //To allow SR to avoid DSF-lookup sending print=false as a @RequestParam to improve performance.
           return Optional.empty()
        }
        kontaktInfoService.setPrintDetails(personResource)
        val dsfResource = kontaktInfoService.getDsfInfo(LookupParameters.lookup(identifier).token(token))
                .orElseThrow { KontaktInfoException("Receiver found in KRR on behalf of '$clientIdentifier', but not in DSF.") }
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
        val senderEntity: Optional<EntityInfo> = entityService.getEntityInfo(clientIdentifier)
        val returnAddress = if (senderEntity.isPresent && senderEntity.get() is OrganizationInfo) {
            val orginfo = senderEntity.get() as OrganizationInfo
            PostAddress(orginfo.organizationName,
                    orginfo.postadresse.adresse,
                    orginfo.postadresse.postnummer,
                    orginfo.postadresse.poststed,
                    orginfo.postadresse.land)
        } else {
            throw BrregNotFoundException(String.format("Sender with identifier=%s not found in BRREG", clientIdentifier))
        }
        val printRecord = SikkerDigitalPostServiceRecord(identifier, p, personResource,
                properties.dpi.endpointURL.toString(), true, postAddress, returnAddress)
        documentTypeService.findByBusinessMessageType(BusinessMessageTypes.PRINT)
                .orElseThrow { missingDocTypeException(BusinessMessageTypes.PRINT) }
                .let { printRecord.documentTypes = listOf(it.identifier) }
        return Optional.of(printRecord)
    }

    fun createDpfioServiceRecord(kontoId: String, protocol: String): ServiceRecord {
        val record = ServiceRecord(ServiceIdentifier.DPFIO, kontoId, protocol, kontoId)
        record.service.serviceCode = protocol
        return record
    }


    fun createDpvServiceRecord(identifier: Iso6523, process: Process): ServiceRecord {
        val dpvServiceRecord = ServiceRecord(ServiceIdentifier.DPV, identifier.orgIdentifier(), process, properties.dpv.endpointURL.toString())
        dpvServiceRecord.service.serviceCode = process.serviceCode
        dpvServiceRecord.service.serviceEditionCode = process.serviceEditionCode
        return dpvServiceRecord
    }

    @Throws(CertificateNotFoundException::class)
    fun createDpeServiceRecord(identifier: Iso6523, process: Process): ServiceRecord {
        val serviceRecord = ServiceRecord(DPE, identifier.orgIdentifier(), process, process.serviceCode)
        serviceRecord.pemCertificate = lookupPemCertificate(identifier, DPE)
        return serviceRecord
    }

    private fun lookupPemCertificate(orgnr: Iso6523, si: ServiceIdentifier): String {
        return virkSertService.getCertificate(orgnr, si)
    }

    private fun missingDocTypeException(messageType: BusinessMessageTypes): RuntimeException {
        return RuntimeException("Missing DocumentType for business message type '$messageType'")
    }

    private fun Iso6523.orgIdentifier(): String {
        return if (requestScope.isUsePlainFormat) this.primaryIdentifier else this.identifier
    }

}
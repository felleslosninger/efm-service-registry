package no.difi.meldingsutveksling.serviceregistry.record

import com.google.common.base.Strings
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
import java.no.difi.meldingsutveklsing.serviceregistry.freg.exception.FregGatewayException
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
    fun createDpoServiceRecord(orgnr: String, process: Process): ServiceRecord {
        val serviceRecord = ServiceRecord(DPO, orgnr, process, properties.dpo.endpointURL.toString())
        serviceRecord.pemCertificate = lookupPemCertificate(orgnr, DPO)
        serviceRecord.service.serviceCode = properties.dpo.serviceCode
        serviceRecord.service.serviceEditionCode = properties.dpo.serviceEditionCode
        return serviceRecord
    }

    fun createDpfServiceRecord(orgnr: String, process: Process, securityLevel: Int): ServiceRecord {
        val serviceRecord = ServiceRecord(ServiceIdentifier.DPF, orgnr, process, properties.fiks.svarut.serviceRecordUrl.toString())
        serviceRecord.pemCertificate = try {
            IOUtils.toString(properties.fiks.svarut.certificate.inputStream, StandardCharsets.UTF_8)
        } catch (e: IOException) {
            log.error(SRMarkerFactory.markerFrom(requestScope), "Could not read certificate from {}", properties.fiks.svarut.certificate.toString())
            throw ServiceRegistryException(e)
        }
        serviceRecord.service.securityLevel = securityLevel
        return serviceRecord
    }

    fun createDigitalServiceRecord(personResource: PersonResource, identifier: String, process: Process): ServiceRecord {
        val serviceRecord = SikkerDigitalPostServiceRecord(identifier, process, personResource, properties.dpi.endpointURL.toString(), false, null, null)
        documentTypeService.findByBusinessMessageType(BusinessMessageTypes.DIGITAL)
                .orElseThrow { missingDocTypeException(BusinessMessageTypes.DIGITAL) }
                .let { serviceRecord.documentTypes = listOf(it.identifier) }
        return serviceRecord
    }

    fun createDigitalDpvServiceRecord(identifier: String, process: Process): ServiceRecord {
        val dpvServiceRecord = ServiceRecord(ServiceIdentifier.DPV, identifier, process, properties.dpv.endpointURL.toString())
        val defaultArkivmeldingProcess = processService.defaultArkivmeldingProcess
        dpvServiceRecord.service.serviceCode = defaultArkivmeldingProcess.serviceCode
        dpvServiceRecord.service.serviceEditionCode = defaultArkivmeldingProcess.serviceEditionCode
        documentTypeService.findByBusinessMessageType(BusinessMessageTypes.DIGITAL_DPV)
                .orElseThrow { missingDocTypeException(BusinessMessageTypes.DIGITAL_DPV) }
                .let { dpvServiceRecord.documentTypes = listOf(it.identifier) }
        return dpvServiceRecord
    }

    @Throws(KontaktInfoException::class, BrregNotFoundException::class)
    fun createPrintServiceRecord(identifier: String,
                                 onBehalfOrgnr: String,
                                 token: Jwt,
                                 personResource: PersonResource,
                                 p: Process,
                                 print: Boolean): Optional<ServiceRecord> {
        if(!print) {
            //To allow SR to avoid DSF-lookup sending print=false as a @RequestParam to improve performance.
           return Optional.empty()
        }
        kontaktInfoService.setPrintDetails(personResource)

       val fregGatewayEntity = kontaktInfoService.getFregAdress(LookupParameters.lookup(identifier))
           .orElseThrow { KontaktInfoException("Receiver found in KRR on behalf of '$onBehalfOrgnr', but not in FREG.") }
        if(Objects.isNull(fregGatewayEntity.postadresse)){
            // Some receivers have secret address - skip
            return Optional.empty();
        }

        val name = fregGatewayEntity.navn.fornavn+fregGatewayEntity.navn.mellomnavn+fregGatewayEntity.navn.etternavn
        val addressline = fregGatewayEntity.postadresse.adresselinje.joinToString(separator = " ")
        val postAddress = PostAddress(name,
            addressline,
            fregGatewayEntity.postadresse.postnummer,
            fregGatewayEntity.postadresse.poststed,
            fregGatewayEntity.postadresse.landkode
            )
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


    fun createDpvServiceRecord(orgnr: String, process: Process): ServiceRecord {
        val dpvServiceRecord = ServiceRecord(ServiceIdentifier.DPV, orgnr, process, properties.dpv.endpointURL.toString())
        dpvServiceRecord.service.serviceCode = process.serviceCode
        dpvServiceRecord.service.serviceEditionCode = process.serviceEditionCode
        return dpvServiceRecord
    }

    @Throws(CertificateNotFoundException::class)
    fun createDpeServiceRecord(orgnr: String, process: Process): ServiceRecord {
        val serviceRecord = ServiceRecord(DPE, orgnr, process, process.serviceCode)
        serviceRecord.pemCertificate = lookupPemCertificate(orgnr, DPE)
        return serviceRecord
    }

    private fun lookupPemCertificate(orgnr: String, si: ServiceIdentifier): String {
        return virkSertService.getCertificate(orgnr, si)
    }

    private fun missingDocTypeException(messageType: BusinessMessageTypes): RuntimeException {
        return RuntimeException("Missing DocumentType for business message type '$messageType'")
    }

}
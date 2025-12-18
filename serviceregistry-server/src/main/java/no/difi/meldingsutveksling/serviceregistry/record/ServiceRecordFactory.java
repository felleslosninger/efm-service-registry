package no.difi.meldingsutveksling.serviceregistry.record;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.serviceregistry.CertificateNotFoundException;
import no.difi.meldingsutveksling.serviceregistry.SRRequestScope;
import no.difi.meldingsutveksling.serviceregistry.config.ServiceregistryProperties;
import no.difi.meldingsutveksling.serviceregistry.domain.*;
import no.difi.meldingsutveksling.serviceregistry.domain.Process;
import no.difi.meldingsutveksling.serviceregistry.domain.ServiceIdentifier;
import no.difi.meldingsutveksling.serviceregistry.exceptions.ServiceRegistryException;
import no.difi.meldingsutveksling.serviceregistry.freg.domain.FregGatewayEntity;
import no.difi.meldingsutveksling.serviceregistry.freg.exception.FregGatewayException;
import no.difi.meldingsutveksling.serviceregistry.krr.PersonResource;
import no.difi.meldingsutveksling.serviceregistry.krr.PostAddress;
import no.difi.meldingsutveksling.serviceregistry.logging.SRMarkerFactory;
import no.difi.meldingsutveksling.serviceregistry.service.DocumentTypeService;
import no.difi.meldingsutveksling.serviceregistry.service.EntityService;
import no.difi.meldingsutveksling.serviceregistry.service.ProcessService;
import no.difi.meldingsutveksling.serviceregistry.service.brreg.BrregNotFoundException;
import no.difi.meldingsutveksling.serviceregistry.service.krr.KontaktInfoService;
import no.difi.meldingsutveksling.serviceregistry.service.virksert.VirkSertService;
import org.apache.commons.io.IOUtils;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class ServiceRecordFactory {

    private final ServiceregistryProperties properties;
    private final VirkSertService virkSertService;
    private final ProcessService processService;
    private final DocumentTypeService documentTypeService;
    private final KontaktInfoService kontaktInfoService;
    private final EntityService entityService;
    private final SRRequestScope requestScope;

    public ServiceRecord createDpoServiceRecord(String orgnr, Process process) throws CertificateNotFoundException {
        ServiceRecord serviceRecord = new ServiceRecord(ServiceIdentifier.DPO, orgnr, process, properties.getDpo().getEndpointURL().toString());
        serviceRecord.setPemCertificate(lookupPemCertificate(orgnr, ServiceIdentifier.DPO));
        serviceRecord.getService().setServiceCode(properties.getDpo().getServiceCode());
        serviceRecord.getService().setServiceEditionCode(properties.getDpo().getServiceEditionCode());
        serviceRecord.getService().setResource(properties.getDpo().getResource());
        return serviceRecord;
    }

    public ServiceRecord createDpfServiceRecord(String orgnr, Process process, int securityLevel) {
        ServiceRecord serviceRecord = new ServiceRecord(ServiceIdentifier.DPF, orgnr, process, properties.getFiks().getSvarut().getServiceRecordUrl().toString());
        try {
            serviceRecord.setPemCertificate(IOUtils.toString(properties.getFiks().getSvarut().getCertificate().getInputStream(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            log.error(
                SRMarkerFactory.markerFrom(requestScope),
                "Could not read certificate from {}",
                properties.getFiks().getSvarut().getCertificate().toString()
            );
            throw new ServiceRegistryException(e);
        }
        serviceRecord.getService().setSecurityLevel(securityLevel);
        return serviceRecord;
    }

    public ServiceRecord createDigitalServiceRecord(PersonResource personResource, String identifier, Process process) {
        SikkerDigitalPostServiceRecord serviceRecord = new SikkerDigitalPostServiceRecord(
                identifier,
                process,
                personResource,
                properties.getDpi().getEndpointURL().toString(),
                false,
                null,
                null
        );
        DocumentType docType = documentTypeService.findByBusinessMessageType(BusinessMessageTypes.DIGITAL)
                .orElseThrow(() -> missingDocTypeException(BusinessMessageTypes.DIGITAL));
        serviceRecord.setDocumentTypes(List.of(docType.getIdentifier()));
        return serviceRecord;
    }

    public ServiceRecord createDigitalDpvServiceRecord(String identifier, Process process) {
        ServiceRecord dpvServiceRecord = new ServiceRecord(ServiceIdentifier.DPV, identifier, process, properties.getDpv().getEndpointURL().toString());
        Process defaultArkivmeldingProcess = processService.getDefaultArkivmeldingProcess();
        dpvServiceRecord.getService().setServiceCode(defaultArkivmeldingProcess.getServiceCode());
        dpvServiceRecord.getService().setServiceEditionCode(defaultArkivmeldingProcess.getServiceEditionCode());
        dpvServiceRecord.getService().setResource(defaultArkivmeldingProcess.getResource());
        DocumentType docType = documentTypeService.findByBusinessMessageType(BusinessMessageTypes.DIGITAL_DPV)
                .orElseThrow(() -> missingDocTypeException(BusinessMessageTypes.DIGITAL_DPV));
        dpvServiceRecord.setDocumentTypes(List.of(docType.getIdentifier()));
        return dpvServiceRecord;
    }

    public String createFullname(FregGatewayEntity.Address.Response fregGatewayEntity) {
        String firstname = fregGatewayEntity.getNavn().getFornavn() + " ";
        String middle = (fregGatewayEntity.getNavn().getMellomnavn() != null && !fregGatewayEntity.getNavn().getMellomnavn().isEmpty())
                ? fregGatewayEntity.getNavn().getMellomnavn() + " " : "";
        String surname = fregGatewayEntity.getNavn().getEtternavn();
        return firstname + middle + surname;
    }

    public Optional<ServiceRecord> createPrintServiceRecord(
            String identifier,
            String onBehalfOrgnr,
            Jwt token,
            PersonResource personResource,
            Process p,
            boolean print
    ) throws FregGatewayException, BrregNotFoundException {
        if (!print) {
            //To allow SR to avoid DSF-lookup sending print=false as a @RequestParam to improve performance.
            return Optional.empty();
        }
        kontaktInfoService.setPrintDetails(personResource);

        FregGatewayEntity.Address.Response fregGatewayEntity = kontaktInfoService.getFregAdress(LookupParameters.lookup(identifier))
                .orElseThrow(() -> new FregGatewayException("Receiver found in KRR on behalf of '" + onBehalfOrgnr + "', but not in FREG."));
        if (fregGatewayEntity.getPostadresse() == null) {
            // Some receivers have secret address - skip
            return Optional.empty();
        }

        String name = createFullname(fregGatewayEntity);
        String addressline = String.join(" ", fregGatewayEntity.getPostadresse().getAdresselinje());
        PostAddress postAddress = new PostAddress(
                name,
                addressline,
                fregGatewayEntity.getPostadresse().getPostnummer(),
                fregGatewayEntity.getPostadresse().getPoststed(),
                fregGatewayEntity.getPostadresse().getLandkode()
        );
        Optional<EntityInfo> senderEntity = entityService.getEntityInfo(onBehalfOrgnr);

        PostAddress returnAddress;
        if (senderEntity.isPresent() && senderEntity.get() instanceof OrganizationInfo orginfo) {
            returnAddress = new PostAddress(
                    orginfo.getOrganizationName(),
                    orginfo.getPostadresse().getAdresse(),
                    orginfo.getPostadresse().getPostnummer(),
                    orginfo.getPostadresse().getPoststed(),
                    orginfo.getPostadresse().getLand()
            );
        } else {
            throw new BrregNotFoundException(
                    String.format("Sender with identifier=%s not found in BRREG", onBehalfOrgnr)
            );
        }
        SikkerDigitalPostServiceRecord printRecord = new SikkerDigitalPostServiceRecord(
                identifier, p, personResource,
                properties.getDpi().getEndpointURL().toString(), true, postAddress, returnAddress
        );
        DocumentType docType = documentTypeService.findByBusinessMessageType(BusinessMessageTypes.PRINT)
                .orElseThrow(() -> missingDocTypeException(BusinessMessageTypes.PRINT));
        printRecord.setDocumentTypes(List.of(docType.getIdentifier()));
        return Optional.of(printRecord);
    }

    public ServiceRecord createDpfioServiceRecord(String kontoId, String protocol) {
        ServiceRecord record = new ServiceRecord(ServiceIdentifier.DPFIO, kontoId, protocol, kontoId);
        record.getService().setServiceCode(protocol);
        return record;
    }

    public ServiceRecord createDpvServiceRecord(String orgnr, Process process) {
        ServiceRecord dpvServiceRecord = new ServiceRecord(ServiceIdentifier.DPV, orgnr, process, properties.getDpv().getEndpointURL().toString());
        dpvServiceRecord.getService().setServiceCode(process.getServiceCode());
        dpvServiceRecord.getService().setServiceEditionCode(process.getServiceEditionCode());
        dpvServiceRecord.getService().setResource(process.getResource());
        return dpvServiceRecord;
    }

    public ServiceRecord createDpeServiceRecord(String orgnr, Process process) throws CertificateNotFoundException {
        ServiceRecord serviceRecord = new ServiceRecord(ServiceIdentifier.DPE, orgnr, process, process.getServiceCode());
        serviceRecord.setPemCertificate(lookupPemCertificate(orgnr, ServiceIdentifier.DPE));
        return serviceRecord;
    }

    private String lookupPemCertificate(String orgnr, ServiceIdentifier si) throws CertificateNotFoundException {
        return virkSertService.getCertificate(orgnr, si);
    }

    private RuntimeException missingDocTypeException(BusinessMessageTypes messageType) {
        return new RuntimeException("Missing DocumentType for business message type '" + messageType + "'");
    }
}


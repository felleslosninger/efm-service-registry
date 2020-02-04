package no.difi.meldingsutveksling.serviceregistry.servicerecord;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.Notification;
import no.difi.meldingsutveksling.serviceregistry.CertificateNotFoundException;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryException;
import no.difi.meldingsutveksling.serviceregistry.config.ServiceregistryProperties;
import no.difi.meldingsutveksling.serviceregistry.exceptions.SecurityLevelNotFoundException;
import no.difi.meldingsutveksling.serviceregistry.krr.*;
import no.difi.meldingsutveksling.serviceregistry.model.Process;
import no.difi.meldingsutveksling.serviceregistry.model.*;
import no.difi.meldingsutveksling.serviceregistry.service.DocumentTypeService;
import no.difi.meldingsutveksling.serviceregistry.service.EntityService;
import no.difi.meldingsutveksling.serviceregistry.service.ProcessService;
import no.difi.meldingsutveksling.serviceregistry.service.brreg.BrregNotFoundException;
import no.difi.meldingsutveksling.serviceregistry.service.elma.ELMALookupService;
import no.difi.meldingsutveksling.serviceregistry.service.krr.KrrService;
import no.difi.meldingsutveksling.serviceregistry.service.virksert.VirkSertService;
import no.difi.meldingsutveksling.serviceregistry.svarut.SvarUtClientException;
import no.difi.meldingsutveksling.serviceregistry.svarut.SvarUtService;
import no.difi.meldingsutveksling.serviceregistry.util.SRRequestScope;
import no.difi.vefa.peppol.common.model.ProcessIdentifier;
import no.difi.virksert.client.lang.VirksertClientException;
import org.apache.commons.io.IOUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetails;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static no.difi.meldingsutveksling.serviceregistry.krr.LookupParameters.lookup;
import static no.difi.meldingsutveksling.serviceregistry.logging.SRMarkerFactory.markerFrom;
import static no.difi.meldingsutveksling.serviceregistry.model.ServiceIdentifier.*;

/**
 * Factory method class to create Service Records based on lookup endpoint urls and certificates corresponding to those services
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ServiceRecordFactory {

    private final KrrService krrService;
    private final ServiceregistryProperties properties;
    private final VirkSertService virksertService;
    private final ELMALookupService elmaLookupService;
    private final EntityService entityService;
    private final SvarUtService svarUtService;
    private final ProcessService processService;
    private final DocumentTypeService documentTypeService;
    private final SRRequestScope requestScope;

    public Optional<ServiceRecord> createArkivmeldingServiceRecord(String orgnr, String processIdentifier, Integer targetSecurityLevel) throws SecurityLevelNotFoundException, CertificateNotFoundException, SvarUtClientException {
        Optional<Process> optionalProcess = processService.findByIdentifier(processIdentifier);
        if (optionalProcess.isPresent()) {
            return Optional.ofNullable(createArkivmeldingServiceRecord(orgnr, targetSecurityLevel, optionalProcess.get()));
        }
        return Optional.empty();
    }

    @SuppressWarnings("squid:S1166")
    public List<ServiceRecord> createArkivmeldingServiceRecords(String orgnr, Integer targetSecurityLevel) throws SecurityLevelNotFoundException, CertificateNotFoundException, SvarUtClientException {
        ArrayList<ServiceRecord> serviceRecords = new ArrayList<>();
        Set<Process> arkivmeldingProcesses = processService.findAll(ProcessCategory.ARKIVMELDING);
        for (Process process : arkivmeldingProcesses) {
            ServiceRecord record = createArkivmeldingServiceRecord(orgnr, targetSecurityLevel, process);
            if (null != record) {
                serviceRecords.add(record);
            }
        }
        return serviceRecords;
    }

    private ServiceRecord createArkivmeldingServiceRecord(String orgnr, Integer targetSecurityLevel, Process process) throws SecurityLevelNotFoundException, CertificateNotFoundException, SvarUtClientException {
        ServiceRecord serviceRecord;
        Set<String> processIdentifiers = getSmpRegistrations(orgnr, Sets.newHashSet(process)).stream()
                .map(ProcessIdentifier::getIdentifier)
                .collect(Collectors.toSet());
        if (processIdentifiers.isEmpty()) {
            if (properties.getFeature().isEnableDpfDpv()) {
                Optional<Integer> hasSvarUt = svarUtService.hasSvarUtAdressering(orgnr, targetSecurityLevel);
                if (hasSvarUt.isPresent()) {
                    serviceRecord = createDpfServiceRecord(orgnr, process, hasSvarUt.get());
                } else {
                    if (targetSecurityLevel != null && targetSecurityLevel == 4) {
                        throw new SecurityLevelNotFoundException(String.format("Organization '%s' can not receive messages with security level '%s'", orgnr, targetSecurityLevel));
                    } else {
                        serviceRecord = createDpvServiceRecord(orgnr, process);
                    }
                }
            } else {
                return null;
            }
        } else {
            if (processIdentifiers.contains(process.getIdentifier())) {
                serviceRecord = createDpoServiceRecord(orgnr, process);
            } else {
                if (targetSecurityLevel != null && targetSecurityLevel == 4) {
                    return null;
                }
                serviceRecord = createDpvServiceRecord(orgnr, process);
            }
        }
        return serviceRecord;
    }


    public Optional<ServiceRecord> createEinnsynServiceRecord(String orgnr, String processIdentifier) throws CertificateNotFoundException {
        Optional<ServiceRecord> optionalServiceRecord = Optional.empty();
        Optional<Process> optionalProcess = processService.findByIdentifier(processIdentifier);
        if (!optionalProcess.isPresent()) {
            return Optional.empty();
        }

        Process process = optionalProcess.get();
        Set<ProcessIdentifier> processIdentifiers = getSmpRegistrations(orgnr, Sets.newHashSet(process));
        if (processIdentifiers.isEmpty()) {
            return Optional.empty();
        }
        if (processIdentifiers.stream()
                .map(ProcessIdentifier::getIdentifier)
                .anyMatch(identifier -> identifier.equals(processIdentifier))) {
            optionalServiceRecord = Optional.of(createDpeServiceRecord(orgnr, process));
        }

        return optionalServiceRecord;
    }

    private ServiceRecord createDpoServiceRecord(String orgnr, Process process) throws CertificateNotFoundException {
        String pem = lookupPemCertificate(orgnr);
        ServiceRecord arkivmeldingServiceRecord = ArkivmeldingServiceRecord.of(DPO, orgnr, properties.getDpo().getEndpointURL().toString(), pem);
        arkivmeldingServiceRecord.setProcess(process.getIdentifier());
        arkivmeldingServiceRecord.getService().setServiceCode(properties.getDpo().getServiceCode());
        arkivmeldingServiceRecord.getService().setServiceEditionCode(properties.getDpo().getServiceEditionCode());
        arkivmeldingServiceRecord.setDocumentTypes(process.getDocumentTypes().stream().map(DocumentType::getIdentifier).collect(Collectors.toList()));

        return arkivmeldingServiceRecord;
    }

    private ServiceRecord createDpfServiceRecord(String orgnr, Process process, Integer securityLevel) {
        String pem;
        try {
            pem = IOUtils.toString(properties.getSvarut().getCertificate().getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error(markerFrom(requestScope), "Could not read certificate from {}", properties.getSvarut().getCertificate().toString());
            throw new ServiceRegistryException(e);
        }
        ServiceRecord arkivmeldingServiceRecord = ArkivmeldingServiceRecord.of(DPF, orgnr, properties.getSvarut().getServiceRecordUrl().toString(), pem);
        arkivmeldingServiceRecord.setProcess(process.getIdentifier());
        arkivmeldingServiceRecord.setDocumentTypes(process.getDocumentTypes().stream().map(DocumentType::getIdentifier).collect(Collectors.toList()));
        arkivmeldingServiceRecord.getService().setSecurityLevel(securityLevel);
        return arkivmeldingServiceRecord;
    }

    private ServiceRecord createDpeServiceRecord(String orgnr, Process process) throws CertificateNotFoundException {
        String pemCertificate = lookupPemCertificate(orgnr);
        ServiceRecord einnsynServiceRecord = DpeServiceRecord.of(pemCertificate, orgnr, DPE, process.getServiceCode());
        einnsynServiceRecord.setProcess(process.getIdentifier());
        einnsynServiceRecord.setDocumentTypes(process.getDocumentTypes().stream().map(DocumentType::getIdentifier).collect(Collectors.toList()));

        return einnsynServiceRecord;
    }


    public List<ServiceRecord> createEinnsynServiceRecords(String orgnr) throws CertificateNotFoundException {
        ArrayList<ServiceRecord> serviceRecords = new ArrayList<>();
        Set<Process> einnsynProcesses = processService.findAll(ProcessCategory.EINNSYN);

        Set<String> processIdentifiers = getSmpRegistrations(orgnr, einnsynProcesses).stream()
                .map(ProcessIdentifier::getIdentifier)
                .collect(Collectors.toSet());

        for (Process p : einnsynProcesses) {
            if (processIdentifiers.contains(p.getIdentifier())) {
                serviceRecords.add(createDpeServiceRecord(orgnr, p));
            }
        }

        return serviceRecords;
    }

    private ArkivmeldingServiceRecord createDpvServiceRecord(String orgnr, Process process) {
        ArkivmeldingServiceRecord dpvServiceRecord = ArkivmeldingServiceRecord.of(DPV, orgnr, properties.getDpv().getEndpointURL().toString());
        dpvServiceRecord.getService().setServiceCode(process.getServiceCode());
        dpvServiceRecord.getService().setServiceEditionCode(process.getServiceEditionCode());
        dpvServiceRecord.setProcess(process.getIdentifier());
        dpvServiceRecord.setDocumentTypes(process.getDocumentTypes().stream().map(DocumentType::getIdentifier).collect(Collectors.toList()));
        return dpvServiceRecord;
    }

    private String lookupPemCertificate(String orgnumber) throws CertificateNotFoundException {
        try {
            return virksertService.getCertificate(orgnumber);
        } catch (VirksertClientException e) {
            throw new CertificateNotFoundException(String.format("Unable to find certificate for: %s", orgnumber), e);
        }
    }

    @PreAuthorize("#oauth2.hasScope('move/dpi.read')")
    public List<ServiceRecord> createDigitalpostServiceRecords(String identifier,
                                                               Authentication auth,
                                                               String onBehalfOrgnr) throws KRRClientException, DsfLookupException, BrregNotFoundException {

        String token = ((OAuth2AuthenticationDetails) auth.getDetails()).getTokenValue();
        PersonResource personResource = krrService.getCitizenInfo(lookup(identifier).token(token));

        Set<Process> digitalpostProcesses = processService.findAll(ProcessCategory.DIGITALPOST);
        List<ServiceRecord> serviceRecords = Lists.newArrayList();
        for (Process p : digitalpostProcesses) {
            DpiMessageRouter.TargetRecord target;
            if (p.getIdentifier().equals(properties.getDpi().getInfoProcess())) {
                target = DpiMessageRouter.route(personResource, Notification.NOT_OBLIGATED);
            } else if (p.getIdentifier().equals(properties.getDpi().getVedtakProcess())) {
                target = DpiMessageRouter.route(personResource, Notification.OBLIGATED);
            } else {
                throw new ServiceRegistryException(String.format("Error processing unknown digitalpost process: %s", p.getIdentifier()));
            }

            switch (target) {
                case DPI:
                    serviceRecords.add(createDigitalServiceRecord(personResource, identifier, p));
                    break;
                case PRINT:
                    createPrintServiceRecord(identifier, onBehalfOrgnr, token, personResource, p).ifPresent(serviceRecords::add);
                    break;
                case DPV:
                default:
                    createPrintServiceRecord(identifier, onBehalfOrgnr, token, personResource, p).ifPresent(serviceRecords::add);
                    serviceRecords.add(createDigitalDpvServiceRecord(identifier, p));
            }
        }

        return serviceRecords;
    }

    private ServiceRecord createDigitalDpvServiceRecord(String identifier, Process process) {
        ArkivmeldingServiceRecord dpvServiceRecord = ArkivmeldingServiceRecord.of(DPV, identifier, properties.getDpv().getEndpointURL().toString());
        Process defaultArkivmeldingProcess = processService.getDefaultArkivmeldingProcess();
        dpvServiceRecord.getService().setServiceCode(defaultArkivmeldingProcess.getServiceCode());
        dpvServiceRecord.getService().setServiceEditionCode(defaultArkivmeldingProcess.getServiceEditionCode());
        dpvServiceRecord.setProcess(process.getIdentifier());
        DocumentType docType = documentTypeService.findByBusinessMessageType(BusinessMessageTypes.DIGITAL_DPV)
                .orElseThrow(this::getServiceRegistryException);
        dpvServiceRecord.setDocumentTypes(Lists.newArrayList(docType.getIdentifier()));
        return dpvServiceRecord;
    }

    private ServiceRecord createDigitalServiceRecord(PersonResource personResource, String identifier, Process p) {
        SikkerDigitalPostServiceRecord serviceRecord = new SikkerDigitalPostServiceRecord(false, properties, personResource, ServiceIdentifier.DPI,
                identifier, null, null);
        serviceRecord.setProcess(p.getIdentifier());
        DocumentType docType = documentTypeService.findByBusinessMessageType(BusinessMessageTypes.DIGITAL)
                .orElseThrow(this::getServiceRegistryException);
        serviceRecord.setDocumentTypes(Lists.newArrayList(docType.getIdentifier()));

        return serviceRecord;
    }

    private Optional<ServiceRecord> createPrintServiceRecord(String identifier,
                                                   String onBehalfOrgnr,
                                                   String token,
                                                   PersonResource personResource,
                                                   Process p) throws KRRClientException, DsfLookupException, BrregNotFoundException {

        krrService.setPrintDetails(personResource);
        Optional<DSFResource> dsfResource = krrService.getDSFInfo(lookup(identifier).token(token));
        if (!dsfResource.isPresent()) {
            throw new DsfLookupException(String.format("Receiver found in KRR on behalf of '%s', but not in DSF.", onBehalfOrgnr));
        }
        if (Strings.isNullOrEmpty(dsfResource.get().getPostAddress())) {
            // Some receivers have secret address - skip
            return Optional.empty();
        }
        String[] codeArea = dsfResource.get().getPostAddress().split(" ");
        PostAddress postAddress = new PostAddress(dsfResource.get().getName(),
                dsfResource.get().getStreet(),
                codeArea[0],
                codeArea.length > 1 ? codeArea[1] : codeArea[0],
                dsfResource.get().getCountry());

        Optional<EntityInfo> senderEntity = entityService.getEntityInfo(onBehalfOrgnr);
        PostAddress returnAddress;
        if (senderEntity.isPresent() && senderEntity.get() instanceof OrganizationInfo) {
            OrganizationInfo orginfo = (OrganizationInfo) senderEntity.get();
            returnAddress = new PostAddress(orginfo.getOrganizationName(),
                    orginfo.getPostadresse().getAdresse(),
                    orginfo.getPostadresse().getPostnummer(),
                    orginfo.getPostadresse().getPoststed(),
                    orginfo.getPostadresse().getLand());
        } else {
            throw new BrregNotFoundException(String.format("Sender with identifier=%s not found in BRREG", onBehalfOrgnr));
        }

        SikkerDigitalPostServiceRecord dpiServiceRecord = new SikkerDigitalPostServiceRecord(true, properties, personResource, ServiceIdentifier.DPI,
                identifier, postAddress, returnAddress);
        DocumentType docType = documentTypeService.findByBusinessMessageType(BusinessMessageTypes.PRINT)
                .orElseThrow(this::getServiceRegistryException);
        dpiServiceRecord.setDocumentTypes(Lists.newArrayList(docType.getIdentifier()));
        dpiServiceRecord.setProcess(p.getIdentifier());
        return Optional.of(dpiServiceRecord);
    }

    private ServiceRegistryException getServiceRegistryException() {
        return new ServiceRegistryException(String.format("Missing DocumentType for business message type '%s'", BusinessMessageTypes.DIGITAL));
    }

    private Set<ProcessIdentifier> getSmpRegistrations(String organizationIdentifier, Set<Process> processes) {
        Set<String> documentTypeIdentifiers = processes.stream()
                .flatMap(p -> p.getDocumentTypes().stream())
                .map(DocumentType::getIdentifier)
                .collect(Collectors.toSet());
        String norwegianIcd = properties.getElma().getLookupIcd();
        return elmaLookupService.lookupRegisteredProcesses(String.format("%s:%s", norwegianIcd, organizationIdentifier), documentTypeIdentifiers);
    }
}

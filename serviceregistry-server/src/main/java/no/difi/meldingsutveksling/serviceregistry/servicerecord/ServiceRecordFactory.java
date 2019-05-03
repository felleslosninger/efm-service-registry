package no.difi.meldingsutveksling.serviceregistry.servicerecord;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.Notification;
import no.difi.meldingsutveksling.logging.MarkerFactory;
import no.difi.meldingsutveksling.serviceregistry.CertificateNotFoundException;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryException;
import no.difi.meldingsutveksling.serviceregistry.config.ServiceregistryProperties;
import no.difi.meldingsutveksling.serviceregistry.exceptions.EndpointUrlNotFound;
import no.difi.meldingsutveksling.serviceregistry.exceptions.SecurityLevelNotFoundException;
import no.difi.meldingsutveksling.serviceregistry.krr.DSFResource;
import no.difi.meldingsutveksling.serviceregistry.krr.KRRClientException;
import no.difi.meldingsutveksling.serviceregistry.krr.PersonResource;
import no.difi.meldingsutveksling.serviceregistry.krr.PostAddress;
import no.difi.meldingsutveksling.serviceregistry.model.Process;
import no.difi.meldingsutveksling.serviceregistry.model.*;
import no.difi.meldingsutveksling.serviceregistry.service.EntityService;
import no.difi.meldingsutveksling.serviceregistry.service.ProcessService;
import no.difi.meldingsutveksling.serviceregistry.service.elma.ELMALookupService;
import no.difi.meldingsutveksling.serviceregistry.service.krr.KrrService;
import no.difi.meldingsutveksling.serviceregistry.service.virksert.VirkSertService;
import no.difi.meldingsutveksling.serviceregistry.svarut.SvarUtService;
import no.difi.vefa.peppol.common.model.ProcessIdentifier;
import no.difi.vefa.peppol.common.model.ServiceMetadata;
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
import static no.difi.meldingsutveksling.serviceregistry.model.ServiceIdentifier.*;

/**
 * Factory method class to create Service Records based on lookup endpoint urls and certificates corresponding to those services
 */
@Component
@Slf4j
public class ServiceRecordFactory {

    private final KrrService krrService;
    private ServiceregistryProperties properties;
    private VirkSertService virksertService;
    private ELMALookupService elmaLookupService;
    private EntityService entityService;
    private SvarUtService svarUtService;
    private ProcessService processService;
    private static final String NORWAY_PREFIX = "9908:";

    /**
     * Creates factory to create ServiceRecord using provided environment and services
     *
     * @param properties        - parameters needed to contact the provided services
     * @param virksertService   - used to lookup virksomhetssertifikat (certificate)
     * @param elmaLookupService - used to lookup hostname of Altinn formidlingstjeneste
     * @param krrService        - used to lookup parameters needed to use DPI transportation
     * @param entityService     - used to look up information about citizens and organizations in Brønnøysundregisteret and Datahotellet.
     * @param svarUtService     - used to determine whether an organization utilizes FIKS.
     */
    public ServiceRecordFactory(ServiceregistryProperties properties,
                                VirkSertService virksertService,
                                ELMALookupService elmaLookupService,
                                KrrService krrService,
                                EntityService entityService,
                                SvarUtService svarUtService,
                                ProcessService processService) {
        this.properties = properties;
        this.virksertService = virksertService;
        this.elmaLookupService = elmaLookupService;
        this.krrService = krrService;
        this.entityService = entityService;
        this.svarUtService = svarUtService;
        this.processService = processService;
    }

    public Optional<ServiceRecord> createArkivmeldingServiceRecord(String orgnr, String processIdentifier, Integer targetSecurityLevel) throws SecurityLevelNotFoundException {
        Optional<Process> optionalProcess = processService.findByIdentifier(processIdentifier);
        if (!optionalProcess.isPresent()) {
            return Optional.empty();
        }
        Optional<ServiceRecord> arkivmeldingServiceRecord;
        Process p = optionalProcess.get();
        Set<ProcessIdentifier> pids = Sets.newHashSet();
        Set<ProcessIdentifier> processIdentifiers = elmaLookup(orgnr, p, pids);

        if (processIdentifiers.isEmpty()) {
            Optional<Integer> hasSvarUt = svarUtService.hasSvarUtAdressering(orgnr, targetSecurityLevel);
            if (hasSvarUt.isPresent()) {
                arkivmeldingServiceRecord = Optional.of(createDpfServiceRecord(orgnr, p, targetSecurityLevel));
            } else {
                if (targetSecurityLevel == null) {
                    arkivmeldingServiceRecord = Optional.of(createDpvServiceRecord(orgnr, p));
                } else {
                    throw new SecurityLevelNotFoundException(String.format("Organization '%s' can not receive messages with security level '%s'", orgnr, targetSecurityLevel));
                }
            }
            return arkivmeldingServiceRecord;
        }

        if (processIdentifiers.stream()
                .map(ProcessIdentifier::getIdentifier)
                .anyMatch(identifier -> identifier.equals(processIdentifier))) {
            arkivmeldingServiceRecord = Optional.of(createDpoServiceRecord(orgnr, p));
        } else {
            arkivmeldingServiceRecord = Optional.of(createDpvServiceRecord(orgnr, p));
        }

        return arkivmeldingServiceRecord;
    }

    @SuppressWarnings("squid:S1166")
    public List<ServiceRecord> createArkivmeldingServiceRecords(String orgnr, Integer targetSecurityLevel) throws SecurityLevelNotFoundException {
        ArrayList<ServiceRecord> serviceRecords = new ArrayList<>();
        List<Process> arkivmeldingProcesses = processService.findAll(ProcessCategory.ARKIVMELDING);
        Set<ProcessIdentifier> smpRegistrations = getSmpRegistrations(orgnr, arkivmeldingProcesses);
        if (smpRegistrations.isEmpty()) {
            Optional<Integer> svarUtRegistration = svarUtService.hasSvarUtAdressering(orgnr, targetSecurityLevel);
            if (svarUtRegistration.isPresent()) {
                arkivmeldingProcesses.forEach(p -> serviceRecords.add(createDpfServiceRecord(orgnr, p, targetSecurityLevel)));
            } else {
                if (null == targetSecurityLevel) {
                    arkivmeldingProcesses.forEach(p -> serviceRecords.add(createDpvServiceRecord(orgnr, p)));
                } else {
                    throw new SecurityLevelNotFoundException(String.format("Organization '%s' can not receive messages with security level '%s'", orgnr, targetSecurityLevel));
                }
            }
        } else {
            List<String> smpProcessIdentifiers = smpRegistrations.stream()
                    .map(ProcessIdentifier::getIdentifier)
                    .collect(Collectors.toList());
            arkivmeldingProcesses.forEach(p -> {
                if (smpProcessIdentifiers.contains(p.getIdentifier())) {
                    serviceRecords.add(createDpoServiceRecord(orgnr, p));
                } else {
                    serviceRecords.add(createDpvServiceRecord(orgnr, p));
                }
            });
        }
        return serviceRecords;
    }

    private Set<ProcessIdentifier> getSmpRegistrations(String organizationIdentifier, List<Process> processes) {
        Set<ProcessIdentifier> processIdentifiers = Sets.newHashSet();
        try {
            List<String> documentTypeIdentifiers = new ArrayList<>();
            for (Process p : processes) {
                p.getDocumentTypes().forEach(t -> {
                    String identifier = t.getIdentifier();
                    if (!documentTypeIdentifiers.contains(identifier)) {
                        documentTypeIdentifiers.add(identifier);
                    }
                });
            }
            List<ServiceMetadata> serviceMetadataList = elmaLookupService.lookup(NORWAY_PREFIX + organizationIdentifier, documentTypeIdentifiers);
            serviceMetadataList.forEach(smd -> smd.getProcesses().forEach(p -> processIdentifiers.add(p.getProcessIdentifier())));
        } catch (EndpointUrlNotFound endpointUrlNotFound) {
            log.debug(MarkerFactory.receiverMarker(organizationIdentifier),
                    String.format("Attempted to lookup receiver in ELMA: %s", endpointUrlNotFound.getMessage()));
        }
        return processIdentifiers;
    }

    public Optional<ServiceRecord> createEinnsynServiceRecord(String orgnr, String processIdentifier) {
        Optional<ServiceRecord> optionalServiceRecord = Optional.empty();
        Optional<Process> optionalProcess = processService.findByIdentifier(processIdentifier);
        if (!optionalProcess.isPresent()) {
            return Optional.empty();
        }
        Process p = optionalProcess.get();
        Set<ProcessIdentifier> pids = Sets.newHashSet();
        Set<ProcessIdentifier> processIdentifiers = elmaLookup(orgnr, p, pids);

        if (processIdentifier.isEmpty()) {
            return Optional.empty();
        }
        if (processIdentifiers.stream()
                .map(ProcessIdentifier::getIdentifier)
                .anyMatch(identifier -> identifier.equals(processIdentifier))) {
            optionalServiceRecord = Optional.of(createDpeServiceRecord(orgnr, p));
        }

        return optionalServiceRecord;
    }

    private ServiceRecord createDpoServiceRecord(String orgnr, Process process) {
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
            log.error("Could not read certificate from {}", properties.getSvarut().getCertificate().toString());
            throw new ServiceRegistryException(e);
        }
        ServiceRecord arkivmeldingServiceRecord = ArkivmeldingServiceRecord.of(DPF, orgnr, properties.getSvarut().getServiceRecordUrl().toString(), pem);
        arkivmeldingServiceRecord.setProcess(process.getIdentifier());
        arkivmeldingServiceRecord.setDocumentTypes(process.getDocumentTypes().stream().map(DocumentType::getIdentifier).collect(Collectors.toList()));
        arkivmeldingServiceRecord.getService().setSecurityLevel(securityLevel);
        return arkivmeldingServiceRecord;
    }

    private ServiceRecord createDpeServiceRecord(String orgnr, Process process) {
        String pemCertificate = lookupPemCertificate(orgnr);
        ServiceRecord einnsynServiceRecord = DpeServiceRecord.of(pemCertificate, orgnr, DPE, process.getServiceCode());
        einnsynServiceRecord.setProcess(process.getIdentifier());
        einnsynServiceRecord.setDocumentTypes(process.getDocumentTypes().stream().map(DocumentType::getIdentifier).collect(Collectors.toList()));

        return einnsynServiceRecord;
    }


    public List<ServiceRecord> createEinnsynServiceRecords(String orgnr) {
        ArrayList<ServiceRecord> serviceRecords = new ArrayList<>();
        Optional<ServiceRecord> optionalServiceRecord;
        List<Process> einnsynProcesses = processService.findAll(ProcessCategory.EINNSYN);
        Set<String> documentTypeIdentifiers = einnsynProcesses.stream()
                .map(Process::getDocumentTypes)
                .flatMap(List::stream)
                .map(DocumentType::getIdentifier)
                .collect(Collectors.toSet());

        List<ServiceMetadata> serviceMetadataList = null;
        Set<ProcessIdentifier> processIdentifiers = Sets.newHashSet();
        try {
            serviceMetadataList = elmaLookupService.lookup(NORWAY_PREFIX + orgnr, Lists.newArrayList(documentTypeIdentifiers));
            serviceMetadataList.forEach(smd -> smd.getProcesses().forEach(p -> processIdentifiers.add(p.getProcessIdentifier())));
        } catch (EndpointUrlNotFound endpointUrlNotFound) {
            log.debug(MarkerFactory.receiverMarker(orgnr),
                    String.format("Attempted to lookup receiver in ELMA: %s", endpointUrlNotFound.getMessage()));
            return serviceRecords;
        }
        if (processIdentifiers.isEmpty()) {
            return serviceRecords;
        }

        for (Process p : einnsynProcesses) {
            if (processIdentifiers.stream()
                    .map(ProcessIdentifier::getIdentifier)
                    .anyMatch(identifier -> identifier.equals(p.getIdentifier()))) {
                optionalServiceRecord = Optional.ofNullable(createDpeServiceRecord(orgnr, p));
                optionalServiceRecord.ifPresent(serviceRecords::add);
            }
        }

        return null;
    }


    private ArkivmeldingServiceRecord createDpvServiceRecord(String orgnr, Process process) {
        ArkivmeldingServiceRecord dpvServiceRecord = ArkivmeldingServiceRecord.of(DPV, orgnr, properties.getDpv().getEndpointURL().toString());
        dpvServiceRecord.getService().setServiceCode(process.getServiceCode());
        dpvServiceRecord.getService().setServiceEditionCode(process.getServiceEditionCode());
        dpvServiceRecord.setProcess(process.getIdentifier());
        dpvServiceRecord.setDocumentTypes(process.getDocumentTypes().stream().map(DocumentType::getIdentifier).collect(Collectors.toList()));
        return dpvServiceRecord;
    }

    private String lookupPemCertificate(String orgnumber) {
        try {
            return virksertService.getCertificate(orgnumber);
        } catch (VirksertClientException e) {
            throw new CertificateNotFoundException(String.format("Unable to find certificate for: %s", orgnumber), e);
        }
    }

    @PreAuthorize("#oauth2.hasScope('move/dpi.read')")
    public List<ServiceRecord> createDigitalpostServiceRecords(String identifier,
                                                               Authentication auth,
                                                               String onBehalfOrgnr,
                                                               Notification notification,
                                                               boolean forcePrint) throws KRRClientException {

        String token = ((OAuth2AuthenticationDetails) auth.getDetails()).getTokenValue();

        PersonResource personResource = krrService.getCitizenInfo(lookup(identifier)
                .onBehalfOf(onBehalfOrgnr)
                .require(notification)
                .token(token));

        // TODO sette prosesser for DPI print og digital
        switch (DpiMessageRouter.route(personResource, notification, forcePrint)) {
            case DPI:
                return createDigitalServiceRecords(personResource, identifier);
            case PRINT:
                return createPrintServiceRecords(identifier, onBehalfOrgnr, token, personResource);
            case DPV:
            default:
                return Lists.newArrayList(createDpvServiceRecord(identifier, processService.getDefaultArkivmeldingProcess()));
        }

    }


    private List<ServiceRecord> createDigitalServiceRecords(PersonResource personResource, String identifier) {
        List<Process> processes = processService.findAll(ProcessCategory.DIGITALPOST);
        List<ServiceRecord> serviceRecords = Lists.newArrayList();
        processes.forEach(p -> {
            SikkerDigitalPostServiceRecord serviceRecord = new SikkerDigitalPostServiceRecord(properties, personResource, ServiceIdentifier.DPI,
                    identifier, null, null);
            serviceRecord.setProcess(p.getIdentifier());
            serviceRecord.setDocumentTypes(Lists.newArrayList(properties.getDpi().getDigitalDocumentType()));
        });

        return serviceRecords;
    }

    private List<ServiceRecord> createPrintServiceRecords(String identifier,
                                                          String onBehalfOrgnr,
                                                          String token,
                                                          PersonResource personResource) throws KRRClientException {

        krrService.setPrintDetails(personResource);
        Optional<DSFResource> dsfResource = krrService.getDSFInfo(lookup(identifier).token(token));
        if (!dsfResource.isPresent()) {
            log.error("Receiver found in KRR on behalf of {}, but not in DSF. Defaulting to DPV.", onBehalfOrgnr);
            return Lists.newArrayList(createDpvServiceRecord(identifier, processService.getDefaultArkivmeldingProcess()));
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
            log.error("Sender {} not found in BRREG, could not get post address. Defaulting to DPV.", onBehalfOrgnr);
            return Lists.newArrayList(createDpvServiceRecord(identifier, processService.getDefaultArkivmeldingProcess()));
        }

        List<Process> processes = processService.findAll(ProcessCategory.DIGITALPOST);
        ArrayList<ServiceRecord> serviceRecords = Lists.newArrayList();
        processes.forEach(p -> {
            SikkerDigitalPostServiceRecord dpiServiceRecord = new SikkerDigitalPostServiceRecord(properties, personResource, ServiceIdentifier.DPI,
                    identifier, postAddress, returnAddress);
            dpiServiceRecord.setDocumentTypes(Lists.newArrayList(properties.getDpi().getPrintDocumentType()));
            dpiServiceRecord.setProcess(p.getIdentifier());
            serviceRecords.add(dpiServiceRecord);
        });
        return serviceRecords;
    }

    private Set<ProcessIdentifier> elmaLookup(String orgnr, Process p, Set<ProcessIdentifier> pids) {
        try {
            List<ServiceMetadata> serviceMetadataList = elmaLookupService.lookup(NORWAY_PREFIX + orgnr, p.getDocumentTypes().stream().map(DocumentType::getIdentifier).collect(Collectors.toList()));
            serviceMetadataList.forEach(smd ->
                    smd.getProcesses().forEach(s -> pids.add(s.getProcessIdentifier()))
            );
        } catch (EndpointUrlNotFound endpointUrlNotFound) {
            log.debug(String.format("Failed to lookup process in ELMA: %s", endpointUrlNotFound.getMessage()));
        }
        return pids;
    }
}

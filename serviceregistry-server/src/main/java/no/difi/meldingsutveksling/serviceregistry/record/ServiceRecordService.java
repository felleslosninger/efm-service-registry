package no.difi.meldingsutveksling.serviceregistry.record;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.domain.Iso6523;
import no.difi.meldingsutveksling.domain.PersonIdentifier;
import no.difi.meldingsutveksling.serviceregistry.CertificateNotFoundException;
import no.difi.meldingsutveksling.serviceregistry.SRRequestScope;
import no.difi.meldingsutveksling.serviceregistry.config.ServiceregistryProperties;
import no.difi.meldingsutveksling.serviceregistry.domain.Process;
import no.difi.meldingsutveksling.serviceregistry.domain.*;
import no.difi.meldingsutveksling.serviceregistry.exceptions.SecurityLevelNotFoundException;
import no.difi.meldingsutveksling.serviceregistry.exceptions.ServiceRegistryException;
import no.difi.meldingsutveksling.serviceregistry.krr.KontaktInfoException;
import no.difi.meldingsutveksling.serviceregistry.krr.PersonResource;
import no.difi.meldingsutveksling.serviceregistry.service.ProcessService;
import no.difi.meldingsutveksling.serviceregistry.service.brreg.BrregNotFoundException;
import no.difi.meldingsutveksling.serviceregistry.service.elma.ELMALookupService;
import no.difi.meldingsutveksling.serviceregistry.service.krr.KontaktInfoService;
import no.difi.meldingsutveksling.serviceregistry.svarut.SvarUtClientException;
import no.difi.meldingsutveksling.serviceregistry.svarut.SvarUtService;
import no.difi.vefa.peppol.common.model.ProcessIdentifier;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

import static no.difi.meldingsutveksling.serviceregistry.domain.ProcessCategory.AVTALT;
import static no.difi.meldingsutveksling.serviceregistry.domain.ProcessCategory.EINNSYN;
import static no.difi.meldingsutveksling.serviceregistry.krr.LookupParameters.lookup;

/**
 * Factory method class to create Service Records based on lookup endpoint urls and certificates corresponding to those services
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ServiceRecordService {

    private final KontaktInfoService kontaktInfoService;
    private final ServiceregistryProperties properties;
    private final ELMALookupService elmaLookupService;
    private final SvarUtService svarUtService;
    private final ProcessService processService;
    private final SRRequestScope requestScope;
    private final ServiceRecordFactory serviceRecordFactory;

    public Optional<ServiceRecord> createFiksIoServiceRecord(EntityInfo entityInfo, String protocol) {
        return Optional.of(serviceRecordFactory.createDpfioServiceRecord(entityInfo.getIdentifier(), protocol));
    }

    @SuppressWarnings("squid:S1166")
    public List<ServiceRecord> createArkivmeldingServiceRecords(Iso6523 identifier, Integer securityLevel) throws SecurityLevelNotFoundException, CertificateNotFoundException, SvarUtClientException {
        ArrayList<ServiceRecord> serviceRecords = new ArrayList<>();
        Set<Process> arkivmeldingProcesses = processService.findAll(ProcessCategory.ARKIVMELDING);
        for (Process process : arkivmeldingProcesses) {
            createArkivmeldingServiceRecord(identifier, process, securityLevel)
                .ifPresent(serviceRecords::add);
        }
        return serviceRecords;
    }

    public Optional<ServiceRecord> createArkivmeldingServiceRecord(Iso6523 identifier, Process process, Integer securityLevel) throws SecurityLevelNotFoundException, CertificateNotFoundException, SvarUtClientException {
        Set<String> processIdentifiers = getSmpRegistrations(identifier, Sets.newHashSet(process)).stream()
            .map(ProcessIdentifier::getIdentifier)
            .collect(Collectors.toSet());
        if (processIdentifiers.isEmpty()) {
            if (properties.getFeature().isEnableDpfDpv()) {
                Optional<Integer> hasSvarUt = svarUtService.hasSvarUtAdressering(identifier, securityLevel);
                if (hasSvarUt.isPresent()) {
                    return Optional.of(serviceRecordFactory.createDpfServiceRecord(identifier, process, hasSvarUt.get()));
                } else {
                    if (securityLevel != null && securityLevel == 4) {
                        throw new SecurityLevelNotFoundException(String.format("Organization '%s' can not receive messages with security level '%s'", identifier, securityLevel));
                    } else {
                        return Optional.of(serviceRecordFactory.createDpvServiceRecord(identifier, process));
                    }
                }
            } else {
                return Optional.empty();
            }
        }

        if (processIdentifiers.contains(process.getIdentifier())) {
            return Optional.of(serviceRecordFactory.createDpoServiceRecord(identifier, process));
        } else {
            if (securityLevel != null && securityLevel == 4) {
                return Optional.empty();
            }
            return Optional.of(serviceRecordFactory.createDpvServiceRecord(identifier, process));
        }
    }

    public Optional<ServiceRecord> createServiceRecord(Iso6523 identifier, Process process) throws CertificateNotFoundException {
        if (getSmpRegistrations(identifier, Sets.newHashSet(process))
            .stream()
            .map(ProcessIdentifier::getIdentifier)
            .anyMatch(i -> i.equals(process.getIdentifier()))) {
            if (process.getCategory() == EINNSYN) {
                return Optional.of(serviceRecordFactory.createDpeServiceRecord(identifier, process));
            } else if (process.getCategory() == AVTALT) {
                return Optional.of(serviceRecordFactory.createDpoServiceRecord(identifier, process));
            }
        }

        return Optional.empty();
    }

    public List<ServiceRecord> createEinnsynServiceRecords(Iso6523 identifier) throws CertificateNotFoundException {
        ArrayList<ServiceRecord> serviceRecords = new ArrayList<>();
        Set<Process> einnsynProcesses = processService.findAll(EINNSYN);

        Set<String> processIdentifiers = getSmpRegistrations(identifier, einnsynProcesses).stream()
            .map(ProcessIdentifier::getIdentifier)
            .collect(Collectors.toSet());

        if (!processIdentifiers.isEmpty()) {
            for (Process p : einnsynProcesses) {
                if (processIdentifiers.contains(p.getIdentifier())) {
                    serviceRecords.add(serviceRecordFactory.createDpeServiceRecord(identifier, p));
                }
            }
            return serviceRecords;
        }

        return serviceRecords;
    }

    public List<ServiceRecord> createAvtaltServiceRecords(Iso6523 identifier) throws CertificateNotFoundException {
        ArrayList<ServiceRecord> serviceRecords = new ArrayList<>();
        Set<Process> avtaltProcesses = processService.findAll(AVTALT);

        Set<String> processIdentifiers = getSmpRegistrations(identifier, avtaltProcesses).stream()
            .map(ProcessIdentifier::getIdentifier)
            .collect(Collectors.toSet());

        for (Process p : avtaltProcesses) {
            if (processIdentifiers.contains(p.getIdentifier())) {
                serviceRecords.add(serviceRecordFactory.createDpoServiceRecord(identifier, p));
            }
        }

        return serviceRecords;
    }

    @PreAuthorize("hasAuthority('SCOPE_move/dpi.read')")
    public List<ServiceRecord> createDigitalpostServiceRecords(PersonIdentifier identifier,
                                                               Iso6523 clientIdentifier,
                                                               boolean print) throws KontaktInfoException, BrregNotFoundException {
        return createDigitalpostServiceRecords(identifier, clientIdentifier, print, processService.findAll(ProcessCategory.DIGITALPOST));
    }

    @PreAuthorize("hasAuthority('SCOPE_move/dpi.read')")
    public List<ServiceRecord> createDigitalpostServiceRecords(PersonIdentifier identifier,
                                                               Iso6523 clientIdentifier,
                                                               boolean print,
                                                               Process process) throws KontaktInfoException, BrregNotFoundException {
        return createDigitalpostServiceRecords(identifier, clientIdentifier, print, Collections.singleton(process));
    }

    private List<ServiceRecord> createDigitalpostServiceRecords(PersonIdentifier identifier,
                                                                Iso6523 clientIdentifier,
                                                                boolean print,
                                                                Set<Process> processes) throws KontaktInfoException, BrregNotFoundException {

        PersonResource personResource = kontaktInfoService.getCitizenInfo(lookup(identifier).token(requestScope.getToken()));
        List<ServiceRecord> serviceRecords = Lists.newArrayList();
        for (Process p : processes) {
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
                    serviceRecords.add(serviceRecordFactory.createDigitalServiceRecord(personResource, identifier, p));
                    break;
                case PRINT:
                    serviceRecordFactory.createPrintServiceRecord(identifier, clientIdentifier, requestScope.getToken(), personResource, p, print)
                        .ifPresent(serviceRecords::add);
                    break;
                case DPV:
                default:
                    serviceRecordFactory.createPrintServiceRecord(identifier, clientIdentifier, requestScope.getToken(), personResource, p, print)
                        .ifPresent(serviceRecords::add);
                    serviceRecords.add(serviceRecordFactory.createDigitalDpvServiceRecord(identifier, p));
            }
        }

        return serviceRecords;
    }

    private Set<ProcessIdentifier> getSmpRegistrations(Iso6523 identifier, Set<Process> processes) {
        Set<String> documentTypeIdentifiers = processes.stream()
            .flatMap(p -> p.getDocumentTypes().stream())
            .map(DocumentType::getIdentifier)
            .collect(Collectors.toSet());
        return elmaLookupService.lookupRegisteredProcesses(identifier, documentTypeIdentifiers);
    }
}

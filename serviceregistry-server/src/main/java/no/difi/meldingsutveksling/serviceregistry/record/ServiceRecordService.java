package no.difi.meldingsutveksling.serviceregistry.record;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.serviceregistry.CertificateNotFoundException;
import no.difi.meldingsutveksling.serviceregistry.SRRequestScope;
import no.difi.meldingsutveksling.serviceregistry.config.ServiceregistryProperties;
import no.difi.meldingsutveksling.serviceregistry.domain.Process;
import no.difi.meldingsutveksling.serviceregistry.domain.*;
import no.difi.meldingsutveksling.serviceregistry.exceptions.SecurityLevelNotFoundException;
import no.difi.meldingsutveksling.serviceregistry.exceptions.ServiceRegistryException;
import no.difi.meldingsutveksling.serviceregistry.freg.domain.FregGatewayEntity;
import no.difi.meldingsutveksling.serviceregistry.freg.exception.FregGatewayException;
import no.difi.meldingsutveksling.serviceregistry.freg.exception.NotFoundInMfGatewayException;
import no.difi.meldingsutveksling.serviceregistry.krr.KontaktInfoException;
import no.difi.meldingsutveksling.serviceregistry.krr.PersonResource;
import no.difi.meldingsutveksling.serviceregistry.service.ProcessService;
import no.difi.meldingsutveksling.serviceregistry.service.brreg.BrregNotFoundException;
import no.difi.meldingsutveksling.serviceregistry.service.dph.ARDetails;
import no.difi.meldingsutveksling.serviceregistry.service.dph.NhnService;
import no.difi.meldingsutveksling.serviceregistry.service.dph.Patient;
import no.difi.meldingsutveksling.serviceregistry.service.elma.ELMALookupService;
import no.difi.meldingsutveksling.serviceregistry.service.krr.KontaktInfoService;
import no.difi.meldingsutveksling.serviceregistry.svarut.SvarUtClientException;
import no.difi.meldingsutveksling.serviceregistry.svarut.SvarUtService;
import network.oxalis.vefa.peppol.common.model.ProcessIdentifier;
import no.idporten.identifiers.validation.PersonIdentifier;
import no.idporten.identifiers.validation.PersonIdentifierValidator;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

import static no.difi.meldingsutveksling.serviceregistry.domain.ProcessCategory.AVTALT;
import static no.difi.meldingsutveksling.serviceregistry.domain.ProcessCategory.EINNSYN;
import static no.difi.meldingsutveksling.serviceregistry.record.LookupParameters.lookup;

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
    private final NhnService nhnService;
    private final SRRequestScope sRRequestScope;
    private final ServiceregistryProperties serviceregistryProperties;

    public Optional<ServiceRecord> createFiksIoServiceRecord(EntityInfo entityInfo, String protocol) {
        return Optional.of(serviceRecordFactory.createDpfioServiceRecord(entityInfo.getIdentifier(), protocol));
    }

    @SuppressWarnings("squid:S1166")
    public List<ServiceRecord> createArkivmeldingServiceRecords(EntityInfo entityInfo, Integer securityLevel) throws SecurityLevelNotFoundException, CertificateNotFoundException, SvarUtClientException {
        ArrayList<ServiceRecord> serviceRecords = new ArrayList<>();
        Set<Process> arkivmeldingProcesses = processService.findAll(ProcessCategory.ARKIVMELDING);
        for (Process process : arkivmeldingProcesses) {
            createArkivmeldingServiceRecord(entityInfo, process, securityLevel)
                .ifPresent(serviceRecords::add);
        }
        return serviceRecords;
    }

    public Optional<ServiceRecord> createArkivmeldingServiceRecord(EntityInfo entityInfo, Process process, Integer securityLevel) throws SecurityLevelNotFoundException, CertificateNotFoundException, SvarUtClientException {
        Set<String> processIdentifiers = getSmpRegistrations(entityInfo.getIdentifier(), Sets.newHashSet(process)).stream()
            .map(ProcessIdentifier::getIdentifier)
            .collect(Collectors.toSet());
        if (processIdentifiers.isEmpty()) {
            if (properties.getFeature().isEnableDpfDpv()) {
                Optional<Integer> hasSvarUt = svarUtService.hasSvarUtAdressering(entityInfo.getIdentifier(), securityLevel);
                if (hasSvarUt.isPresent()) {
                    return Optional.of(serviceRecordFactory.createDpfServiceRecord(entityInfo.getIdentifier(), process, hasSvarUt.get()));
                } else {
                    if (securityLevel != null && securityLevel == 4) {
                        throw new SecurityLevelNotFoundException(String.format("Organization '%s' can not receive messages with security level '%s'", entityInfo, securityLevel));
                    } else {
                        return Optional.of(serviceRecordFactory.createDpvServiceRecord(entityInfo.getIdentifier(), process));
                    }
                }
            } else {
                return Optional.empty();
            }
        }

        if (processIdentifiers.contains(process.getIdentifier())) {
            return Optional.of(serviceRecordFactory.createDpoServiceRecord(entityInfo.getIdentifier(), process));
        } else {
            if (securityLevel != null && securityLevel == 4) {
                return Optional.empty();
            }
            return Optional.of(serviceRecordFactory.createDpvServiceRecord(entityInfo.getIdentifier(), process));
        }
    }

    public Optional<ServiceRecord> createServiceRecord(EntityInfo entityInfo, Process process, Integer securityLevel) throws CertificateNotFoundException {
        if (getSmpRegistrations(entityInfo.getIdentifier(), Sets.newHashSet(process))
            .stream()
            .map(ProcessIdentifier::getIdentifier)
            .anyMatch(identifier -> identifier.equals(process.getIdentifier()))) {
            if (process.getCategory() == EINNSYN) {
                return Optional.of(serviceRecordFactory.createDpeServiceRecord(entityInfo.getIdentifier(), process));
            } else if (process.getCategory() == AVTALT) {
                return Optional.of(serviceRecordFactory.createDpoServiceRecord(entityInfo.getIdentifier(), process));
            }
        }

        return Optional.empty();
    }

    public List<ServiceRecord> createEinnsynServiceRecords(EntityInfo entityInfo, Integer securityLevel) throws CertificateNotFoundException {
        ArrayList<ServiceRecord> serviceRecords = new ArrayList<>();
        Set<Process> einnsynProcesses = processService.findAll(EINNSYN);

        Set<String> processIdentifiers = getSmpRegistrations(entityInfo.getIdentifier(), einnsynProcesses).stream()
            .map(ProcessIdentifier::getIdentifier)
            .collect(Collectors.toSet());

        if (!processIdentifiers.isEmpty()) {
            for (Process p : einnsynProcesses) {
                if (processIdentifiers.contains(p.getIdentifier())) {
                    serviceRecords.add(serviceRecordFactory.createDpeServiceRecord(entityInfo.getIdentifier(), p));
                }
            }
            return serviceRecords;
        }

        return serviceRecords;
    }

    public List<ServiceRecord> createAvtaltServiceRecords(String orgnr) throws CertificateNotFoundException {
        ArrayList<ServiceRecord> serviceRecords = new ArrayList<>();
        Set<Process> avtaltProcesses = processService.findAll(AVTALT);

        Set<String> processIdentifiers = getSmpRegistrations(orgnr, avtaltProcesses).stream()
            .map(ProcessIdentifier::getIdentifier)
            .collect(Collectors.toSet());

        for (Process p : avtaltProcesses) {
            if (processIdentifiers.contains(p.getIdentifier())) {
                serviceRecords.add(serviceRecordFactory.createDpoServiceRecord(orgnr, p));
            }
        }

        return serviceRecords;
    }

    @PreAuthorize("hasAuthority('SCOPE_move/dpi.read')")
    public List<ServiceRecord> createDigitalpostServiceRecords(String identifier,
                                                               String onBehalfOrgnr,
                                                               boolean print) throws KontaktInfoException, BrregNotFoundException, FregGatewayException {
        return createDigitalpostServiceRecords(identifier, onBehalfOrgnr, print, processService.findAll(ProcessCategory.DIGITALPOST));
    }

    public List<ServiceRecord> createDphRecords(EntityInfo entityInfo) throws FregGatewayException{
        Process process;
        if(entityInfo instanceof CitizenInfo)   {
           process = processService.findByIdentifier(serviceregistryProperties.getDph().fastlegeProcess()).orElseThrow(()->new ServiceRegistryException("Fastlege process not found"));
        }
        else if (entityInfo instanceof HelseEnhetInfo) {
            process = processService.findByIdentifier(serviceregistryProperties.getDph().nhnProcess()).orElseThrow(()->new ServiceRegistryException("Nhn process not found"));
        }
        else {
            throw new ServiceRegistryException("Identifier is not compatible with DPH");
        }
        return createDphServiceRecords(entityInfo.getIdentifier(),process);
    }


    private List<ServiceRecord> createDphServiceRecords(String identifier, Process process) throws FregGatewayException {
        LookupParameters param = LookupParameters.lookup(identifier);
        param.setToken(sRRequestScope.getToken());
        ARDetails arDetails = nhnService.getARDetails(param);
        Patient patient = null;
        PersonIdentifierValidator.setSyntheticPersonIdentifiersAllowed(true);
        if (PersonIdentifierValidator.isValid(identifier)) {
            try {
                patient = kontaktInfoService.getFregAdress(param).map(
                        t -> {
                            return new Patient(t.getPersonIdentifikator(), t.getNavn().getFornavn(), t.getNavn().getMellomnavn(), t.getNavn().getEtternavn());
                        }
                ).orElse(null);

            } catch (NotFoundInMfGatewayException e) {
                throw new FregGatewayException("Receiver not found in FREG.", e);
            }
        }
        if (process == null) {
            throw new IllegalArgumentException("processer must contain at least one Process");
        }

        DPHServiceRecord sr = new DPHServiceRecord(ServiceIdentifier.DPH, arDetails.getOrgNumber(), process,arDetails.getEdiAdress(),arDetails.getHerid1(),arDetails.getHerid2(),patient );
        sr.setPemCertificate(arDetails.getPemDigdirSertifikat());
        return List.of(sr);
    }



    @PreAuthorize("hasAuthority('SCOPE_move/dpi.read')")
    public List<ServiceRecord> createDigitalpostServiceRecords(String identifier,
                                                               String onBehalfOrgnr,
                                                               boolean print,
                                                               Process process) throws KontaktInfoException, BrregNotFoundException, FregGatewayException {
        return createDigitalpostServiceRecords(identifier, onBehalfOrgnr, print, Collections.singleton(process));
    }



    private List<ServiceRecord> createDigitalpostServiceRecords(String identifier,
                                                                String onBehalfOrgnr,
                                                                boolean print,
                                                                Set<Process> processes) throws KontaktInfoException, BrregNotFoundException, FregGatewayException {

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
                    serviceRecordFactory.createPrintServiceRecord(identifier, onBehalfOrgnr, requestScope.getToken(), personResource, p, print)
                        .ifPresent(serviceRecords::add);
                    break;
                case DPV:
                default:
                    serviceRecordFactory.createPrintServiceRecord(identifier, onBehalfOrgnr, requestScope.getToken(), personResource, p, print)
                        .ifPresent(serviceRecords::add);
                    serviceRecords.add(serviceRecordFactory.createDigitalDpvServiceRecord(identifier, p));
            }
        }

        return serviceRecords;
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

package no.difi.meldingsutveksling.serviceregistry.record;

import com.google.common.collect.Sets;
import lombok.SneakyThrows;
import no.difi.meldingsutveksling.serviceregistry.CertificateNotFoundException;
import no.difi.meldingsutveksling.serviceregistry.SRRequestScope;
import no.difi.meldingsutveksling.serviceregistry.config.ServiceregistryProperties;
import no.difi.meldingsutveksling.serviceregistry.domain.Process;
import no.difi.meldingsutveksling.serviceregistry.domain.*;
import no.difi.meldingsutveksling.serviceregistry.exceptions.SecurityLevelNotFoundException;
import no.difi.meldingsutveksling.serviceregistry.krr.LookupParameters;
import no.difi.meldingsutveksling.serviceregistry.krr.PersonResource;
import no.difi.meldingsutveksling.serviceregistry.service.ProcessService;
import no.difi.meldingsutveksling.serviceregistry.service.elma.ELMALookupService;
import no.difi.meldingsutveksling.serviceregistry.service.krr.KontaktInfoService;
import no.difi.meldingsutveksling.serviceregistry.svarut.SvarUtClientException;
import no.difi.meldingsutveksling.serviceregistry.svarut.SvarUtService;
import no.difi.vefa.peppol.common.model.ProcessIdentifier;
import no.difi.virksert.client.lang.VirksertClientException;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ServiceRecordServiceTest {

    @InjectMocks
    private ServiceRecordService service;

    @Mock
    private ServiceRecordFactory serviceRecordFactory;

    @Mock
    private ELMALookupService lookupService;

    @Mock
    private KontaktInfoService kontaktInfoService;

    @Mock
    private SvarUtService svarUtService;

    @Mock
    private ProcessService processService;

    @Mock
    private ServiceregistryProperties props;

    @Mock
    private SRRequestScope requestScope;

    private static String ORGNR = "123456789";
    private static String ORGNR_FIKS = "987654321";
    private static String ORGNR_EINNSYN_RESPONSE = "987987987";
    private static String ORGNR_EINNSYN = "123987654";
    private static String PERSONNUMMER = "01234567890";
    private static final String ELMA_LOOKUP_ICD = "0192";
    private static final OrganizationType ORGL = new OrganizationType("ORGL");
    private static final OrganizationType KOMM = new OrganizationType("ORGL");
    private static final EntityInfo ORGNR_ORG = new OrganizationInfo(ORGNR, ORGL);
    private static final EntityInfo ORGNR_FIKS_KOMM = new OrganizationInfo(ORGNR_FIKS, KOMM);

    private static String ARKIVMELDING_PROCESS_ADMIN = "urn:no:difi:profile:arkivmelding:administrasjon:ver1.0";
    private static String ARKIVMELDING_PROCESS_SKATT = "urn:no:difi:profile:arkivmelding:skatterOgAvgifter:ver1.0";
    private static String ARKIVMELDING_DOCTYPE = "urn:no:difi:arkivmelding:xsd::arkivmelding";

    private static String AVTALT_PROCESS = "urn:no:difi:profile:avtalt:avtalt:ver1.0";
    private static String AVTALT_DOCTYPE = "urn:no:difi:avtalt:xsd::avtalt";

    private static String EINNSYN_PROCESS_JOURNALPOST = "urn:no:difi:profile:einnsyn:journalpost:ver1.0";
    private static String EINNSYN_DOCTYPE_JOURNALPOST = "urn:no:difi:einnsyn:xsd::publisering";
    private static String EINNSYN_PROCESS_INNSYNSKRAV = "urn:no:difi:profile:einnsyn:innsynskrav:ver1.0";
    private static String EINNSYN_DOCTYPE_INNSYNSKRAV = "urn:no:difi:einnsyn:xsd::innsynskrav";

    private static String DIGITALPOST_PROCESS_VEDTAK = "urn:no:difi:profile:digitalpost:vedtak:ver1.0";
    private static String DIGITALPOST_PROCESS_INFO = "urn:no:difi:profile:digitalpost:info:ver1.0";
    private static String DIGITALPOST_DOCTYPE_DIGITAL = "urn:no:difi:digitalpost:xsd:digital::digital";
    private static String DIGITALPOST_DOCTYPE_DIGITALDPV = "urn:no:difi:digitalpost:xsd:digital::digital_dpv";
    private static String DIGITALPOST_DOCTYPE_PRINT = "urn:no:difi:digitalpost:xsd:fysisk::print";

    private static Process processAdmin;
    private static Process processAvtalt;
    private static Process processSkatt;
    private static Process processJournalpost;
    private static Process processInnsynskrav;
    private static Process processVedtak;
    private static Process processInfo;

    @BeforeEach
    public void init() {
        ServiceregistryProperties.ELMA elmaProps = new ServiceregistryProperties.ELMA();
        elmaProps.setLookupIcd(ELMA_LOOKUP_ICD);
        lenient().when(props.getElma()).thenReturn(elmaProps);

        // Arkivmelding
        DocumentType documentType = new DocumentType()
            .setIdentifier(ARKIVMELDING_DOCTYPE);
        processAdmin = new Process()
            .setIdentifier(ARKIVMELDING_PROCESS_ADMIN)
            .setCategory(ProcessCategory.ARKIVMELDING)
            .setServiceCode("4192")
            .setServiceEditionCode("270815");
        processSkatt = new Process()
            .setIdentifier(ARKIVMELDING_PROCESS_SKATT)
            .setCategory(ProcessCategory.ARKIVMELDING)
            .setServiceCode("4192")
            .setServiceEditionCode("270815");
        processSkatt.setDocumentTypes(Lists.newArrayList(documentType));
        processAdmin.setDocumentTypes(Lists.newArrayList(documentType));
        documentType.setProcesses(Lists.newArrayList(processAdmin, processSkatt));

        // Avtalt
        DocumentType documentTypeAvtalt = new DocumentType()
            .setIdentifier(AVTALT_DOCTYPE);
        processAvtalt = new Process().setIdentifier(AVTALT_PROCESS)
            .setCategory(ProcessCategory.AVTALT)
            .setServiceCode("4192")
            .setServiceEditionCode("270815");
        processAvtalt.setDocumentTypes(Lists.newArrayList(documentTypeAvtalt));
        documentTypeAvtalt.setProcesses(Lists.newArrayList(processAvtalt));

        // Digital
        processVedtak = new Process()
            .setIdentifier(DIGITALPOST_PROCESS_VEDTAK)
            .setCategory(ProcessCategory.DIGITALPOST)
            .setDocumentTypes(Lists.newArrayList(new DocumentType().setIdentifier(DIGITALPOST_DOCTYPE_PRINT),
                new DocumentType().setIdentifier(DIGITALPOST_DOCTYPE_DIGITALDPV)));
        processInfo = new Process()
            .setIdentifier(DIGITALPOST_PROCESS_INFO)
            .setCategory(ProcessCategory.DIGITALPOST)
            .setDocumentTypes(Lists.newArrayList(new DocumentType().setIdentifier(DIGITALPOST_DOCTYPE_DIGITAL)));

        // Einnsyn
        DocumentType einnsynJournalpostDocumentType = new DocumentType()
            .setIdentifier(EINNSYN_DOCTYPE_JOURNALPOST);
        processJournalpost = new Process()
            .setIdentifier(EINNSYN_PROCESS_JOURNALPOST)
            .setCategory(ProcessCategory.EINNSYN)
            .setServiceCode("data")
            .setDocumentTypes(Lists.newArrayList(einnsynJournalpostDocumentType));
        einnsynJournalpostDocumentType.setProcesses(Lists.newArrayList(processJournalpost));

        DocumentType einnsynInnsynskravDocumentType = new DocumentType()
            .setIdentifier(EINNSYN_DOCTYPE_INNSYNSKRAV);
        processInnsynskrav = new Process()
            .setIdentifier(EINNSYN_PROCESS_INNSYNSKRAV)
            .setCategory(ProcessCategory.EINNSYN)
            .setServiceCode("innsyn")
            .setDocumentTypes(Lists.newArrayList(einnsynInnsynskravDocumentType));
        einnsynInnsynskravDocumentType.setProcesses(Lists.newArrayList(processInnsynskrav));
    }

    private void enablePropertyEnableDpvDpf() {
        ServiceregistryProperties.FeatureToggle feature = new ServiceregistryProperties.FeatureToggle();
        feature.setEnableDpfDpv(true);
        when(props.getFeature()).thenReturn(feature);
    }

    private void processServiceReturnsArkivmeldingProcesses() {
        when(processService.findAll(ProcessCategory.ARKIVMELDING)).thenReturn(Sets.newHashSet(processAdmin, processSkatt));
    }

    private void processServiceReturnsEinnsynProcesses() {
        when(processService.findAll(ProcessCategory.EINNSYN)).thenReturn(Sets.newHashSet(processInnsynskrav, processJournalpost));
    }

    private void lookupServiceReturnsArkivmeldingAdminProcess() {
        when(lookupService.lookupRegisteredProcesses(eq(String.format("%s:%s", ELMA_LOOKUP_ICD, ORGNR)), anySet()))
            .thenReturn(Sets.newHashSet(ProcessIdentifier.of(ARKIVMELDING_PROCESS_ADMIN)));
    }

    private void lookupServiceReturnsAvtaltProcess() {
        when(lookupService.lookupRegisteredProcesses(eq(String.format("%s:%s", ELMA_LOOKUP_ICD, ORGNR)), anySet()))
            .thenReturn(Sets.newHashSet(ProcessIdentifier.of(AVTALT_PROCESS)));
    }

    private void lookupServiceReturnsEinnsynJournalpostProcesses() {
        when(lookupService.lookupRegisteredProcesses(eq(String.format("%s:%s", ELMA_LOOKUP_ICD, ORGNR)), anySet()))
            .thenReturn(Sets.newHashSet(ProcessIdentifier.of(EINNSYN_PROCESS_JOURNALPOST), ProcessIdentifier.of(EINNSYN_PROCESS_INNSYNSKRAV)));
    }

    @SneakyThrows
    @Test
    public void createArkivmeldingServiceRecord_IdentifierHasSvarUtRegistrationOnDifferentSecurityLevel_ShouldThrowDedicatedException() {
        enablePropertyEnableDpvDpf();
        when(svarUtService.hasSvarUtAdressering(anyString(), any())).thenReturn(Optional.empty());
        assertThrows(SecurityLevelNotFoundException.class, () -> service.createArkivmeldingServiceRecord(ORGNR_ORG, mock(Process.class), 4));
    }

    @SneakyThrows
    @Test
    public void createArkivMeldingServiceRecord_OrganizationHasAdministrationInSmp_ShouldReturnDpoServiceRecord() {
        lookupServiceReturnsArkivmeldingAdminProcess();
        when(serviceRecordFactory.createDpoServiceRecord(ORGNR, processAdmin)).thenReturn(mock(ServiceRecord.class));

        service.createArkivmeldingServiceRecord(ORGNR_ORG, processAdmin, null);
        verify(serviceRecordFactory).createDpoServiceRecord(ORGNR, processAdmin);
    }

    @SneakyThrows
    @Test
    public void createAvtaltServiceRecord_OrganizationHasAvtaltInSmp_ShouldReturnDpoServiceRecord() {
        lookupServiceReturnsAvtaltProcess();
        when(serviceRecordFactory.createDpoServiceRecord(ORGNR, processAvtalt)).thenReturn(mock(ServiceRecord.class));

        service.createServiceRecord(ORGNR_ORG, processAvtalt, null);
        verify(serviceRecordFactory).createDpoServiceRecord(ORGNR, processAvtalt);
    }

    @SneakyThrows
    @Test
    public void createArkivmeldingServiceRecord_OrganizationHasSvarUtRegistration_ShouldReturnDpfServiceRecord() {
        enablePropertyEnableDpvDpf();
        when(svarUtService.hasSvarUtAdressering(eq(ORGNR_FIKS), eq(4))).thenReturn(Optional.of(4));
        when(serviceRecordFactory.createDpfServiceRecord(eq(ORGNR_FIKS), eq(processSkatt), eq(4))).thenReturn(mock(ServiceRecord.class));

        service.createArkivmeldingServiceRecord(ORGNR_FIKS_KOMM, processSkatt, 4);
        verify(serviceRecordFactory).createDpfServiceRecord(eq(ORGNR_FIKS), eq(processSkatt), eq(4));
    }

    @SneakyThrows
    @Test
    public void createArkivmeldingServiceRecords_OrganizationHasSvarUtRegistration_ShouldReturnDpfServiceRecord() {
        enablePropertyEnableDpvDpf();
        processServiceReturnsArkivmeldingProcesses();
        when(svarUtService.hasSvarUtAdressering(eq(ORGNR_FIKS), eq(4))).thenReturn(Optional.of(4));
        when(serviceRecordFactory.createDpfServiceRecord(eq(ORGNR_FIKS), any(Process.class), eq(4))).thenReturn(mock(ServiceRecord.class));

        service.createArkivmeldingServiceRecords(ORGNR_FIKS_KOMM, 4);
        verify(serviceRecordFactory, times(2)).createDpfServiceRecord(eq(ORGNR_FIKS), any(Process.class), eq(4));
    }

    @SneakyThrows
    @Test
    public void createArkivmeldingServiceRecord_NoSmpNorSvarUtRegistration_ShouldReturnDpvServiceRecord() {
        enablePropertyEnableDpvDpf();
        when(svarUtService.hasSvarUtAdressering(anyString(), any())).thenReturn(Optional.empty());
        when(serviceRecordFactory.createDpvServiceRecord(any(), any())).thenReturn(mock(ServiceRecord.class));

        service.createArkivmeldingServiceRecord(ORGNR_ORG, processSkatt, null);
        verify(serviceRecordFactory).createDpvServiceRecord(ORGNR_ORG.getIdentifier(), processSkatt);
    }

    @SneakyThrows
    @Test
    public void createArkivmeldingServiceRecords_NoProcessesFound_ShouldReturnEmptyList() {
        when(processService.findAll(ProcessCategory.ARKIVMELDING)).thenReturn(Sets.newHashSet());

        assertTrue(service.createArkivmeldingServiceRecords(ORGNR_ORG, null).isEmpty());
    }

    @SneakyThrows
    @Test
    public void createArkivmeldingServiceRecords_CertificateNotFoundForSmpProcess_ShouldThrowDedicatedException() {
        lookupServiceReturnsArkivmeldingAdminProcess();
        when(serviceRecordFactory.createDpoServiceRecord(ORGNR, processAdmin)).thenThrow(new CertificateNotFoundException(new VirksertClientException("certificate not found")));
        assertThrows(CertificateNotFoundException.class, () -> service.createArkivmeldingServiceRecord(ORGNR_ORG, processAdmin, null));
    }

    @SneakyThrows
    @Test
    public void createArkivmeldingServiceRecords_OrganizationHasAdministrasjonButNotSkattRegistrationInSmp_ShouldReturnCorrespondingDpoAndDpvServiceRecords() {
        lookupServiceReturnsArkivmeldingAdminProcess();
        when(processService.findAll(ProcessCategory.ARKIVMELDING)).thenReturn(Sets.newHashSet(processAdmin, processSkatt));
        when(serviceRecordFactory.createDpoServiceRecord(ORGNR, processAdmin)).thenReturn(mock(ServiceRecord.class));
        when(serviceRecordFactory.createDpvServiceRecord(ORGNR, processSkatt)).thenReturn(mock(ServiceRecord.class));

        service.createArkivmeldingServiceRecords(ORGNR_ORG, null);
        verify(serviceRecordFactory).createDpoServiceRecord(ORGNR, processAdmin);
        verify(serviceRecordFactory).createDpvServiceRecord(ORGNR, processSkatt);
    }

    @SneakyThrows
    @Test
    public void createArkivmeldingServiceRecords_IdentifierHasSvarUtRegistrationOnDifferentSecurityLevel_ShouldThrowDedicatedException() {
        enablePropertyEnableDpvDpf();
        when(processService.findAll(ProcessCategory.ARKIVMELDING)).thenReturn(Sets.newHashSet(processAdmin, processSkatt));
        when(svarUtService.hasSvarUtAdressering(eq(ORGNR_FIKS), any())).thenReturn(Optional.empty());
        when(lookupService.lookupRegisteredProcesses(eq(String.format("%s:%s", ELMA_LOOKUP_ICD, ORGNR_FIKS)), anySet())).thenReturn(Sets.newHashSet());
        assertThrows(SecurityLevelNotFoundException.class, () -> service.createArkivmeldingServiceRecords(ORGNR_FIKS_KOMM, 4));
    }

    @SneakyThrows
    @Test
    public void createArkivmeldingServiceRecords_SvarUtServiceIsUnavailable_ShouldReturnEmpty() {
        enablePropertyEnableDpvDpf();
        when(processService.findAll(ProcessCategory.ARKIVMELDING)).thenReturn(Sets.newHashSet(processAdmin, processSkatt));
        when(svarUtService.hasSvarUtAdressering(eq(ORGNR_FIKS), any()))
            .thenThrow(new SvarUtClientException(new RuntimeException("service unavailable")));
        assertThrows(SvarUtClientException.class, () -> service.createArkivmeldingServiceRecords(ORGNR_FIKS_KOMM, 3));
    }

    @SneakyThrows
    @Test
    public void createEinnsynServiceRecords_ShouldReturnDpeServiceRecords() {
        processServiceReturnsEinnsynProcesses();
        lookupServiceReturnsEinnsynJournalpostProcesses();
        when(serviceRecordFactory.createDpeServiceRecord(eq(ORGNR), any(Process.class))).thenReturn(mock(ServiceRecord.class));
        List<ServiceRecord> result = service.createEinnsynServiceRecords(ORGNR_ORG, null);
        assertEquals(2, result.size());
    }

    @SneakyThrows
    @Test
    public void createEinnsynServiceRecords_OrgnrNotInElma_ShouldNotReturnDpeServiceRecord() {
        processServiceReturnsEinnsynProcesses();
        List<ServiceRecord> result = service.createEinnsynServiceRecords(new OrganizationInfo().setIdentifier(ORGNR_EINNSYN_RESPONSE).setOrganizationType(ORGL), 3);
        assertTrue(result.isEmpty());
    }

    @SneakyThrows
    @Test
    public void createEinnsynServiceRecords_EndpointurlNotFound_ShouldNotReturnDpeServiceRecord() {
        List<ServiceRecord> result = service.createEinnsynServiceRecords(new OrganizationInfo().setIdentifier(ORGNR_EINNSYN).setOrganizationType(ORGL), 3);
        assertTrue(result.isEmpty());
    }

    @SneakyThrows
    @Test
    public void createDigitalServiceRecords_ShouldReturnDigitalRecord() {
        ServiceregistryProperties.DigitalPostInnbygger dpiProps = mock(ServiceregistryProperties.DigitalPostInnbygger.class);
        when(dpiProps.getInfoProcess()).thenReturn(DIGITALPOST_PROCESS_INFO);
        when(props.getDpi()).thenReturn(dpiProps);

        PersonResource personResource = mock(PersonResource.class);
        when(personResource.hasMailbox()).thenReturn(true);
        when(personResource.isActive()).thenReturn(true);

        when(kontaktInfoService.getCitizenInfo(any(LookupParameters.class))).thenReturn(personResource);
        when(serviceRecordFactory.createDigitalServiceRecord(personResource, PERSONNUMMER, processInfo)).thenReturn(mock(ServiceRecord.class));

        List<ServiceRecord> records = service.createDigitalpostServiceRecords(PERSONNUMMER, ORGNR, true, processInfo);
        assertEquals(1, records.size());
    }

    @SneakyThrows
    @Test
    public void createDigitalServiceRecords_ShouldReturnDigitalDpvAndPrintRecords() {
        ServiceregistryProperties.DigitalPostInnbygger dpiProps = mock(ServiceregistryProperties.DigitalPostInnbygger.class);
        when(dpiProps.getInfoProcess()).thenReturn(DIGITALPOST_PROCESS_INFO);
        when(dpiProps.getVedtakProcess()).thenReturn(DIGITALPOST_PROCESS_VEDTAK);
        when(props.getDpi()).thenReturn(dpiProps);

        PersonResource personResource = mock(PersonResource.class);
        when(personResource.isReserved()).thenReturn(false);
        when(personResource.isNotifiable()).thenReturn(true);
        when(personResource.hasMailbox()).thenReturn(false);

        when(kontaktInfoService.getCitizenInfo(any(LookupParameters.class))).thenReturn(personResource);
        when(serviceRecordFactory.createPrintServiceRecord(eq(PERSONNUMMER), eq(ORGNR), any(), eq(personResource), eq(processVedtak), eq(true)))
            .thenReturn(Optional.of(mock(ServiceRecord.class)));
        when(serviceRecordFactory.createDigitalDpvServiceRecord(PERSONNUMMER, processVedtak))
            .thenReturn(mock(ServiceRecord.class));

        assertEquals(2, service.createDigitalpostServiceRecords(PERSONNUMMER, ORGNR, true, processVedtak).size());
    }

}

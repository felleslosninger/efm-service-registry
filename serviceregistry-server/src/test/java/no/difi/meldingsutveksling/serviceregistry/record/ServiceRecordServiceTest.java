package no.difi.meldingsutveksling.serviceregistry.record;

import com.google.common.collect.Sets;
import no.difi.meldingsutveksling.serviceregistry.CertificateNotFoundException;
import no.difi.meldingsutveksling.serviceregistry.SRRequestScope;
import no.difi.meldingsutveksling.serviceregistry.config.ServiceregistryProperties;
import no.difi.meldingsutveksling.serviceregistry.domain.Process;
import no.difi.meldingsutveksling.serviceregistry.domain.*;
import no.difi.meldingsutveksling.serviceregistry.exceptions.ProcessNotFoundException;
import no.difi.meldingsutveksling.serviceregistry.exceptions.SecurityLevelNotFoundException;
import no.difi.meldingsutveksling.serviceregistry.fiks.io.FiksIoService;
import no.difi.meldingsutveksling.serviceregistry.fiks.io.FiksProtocol;
import no.difi.meldingsutveksling.serviceregistry.fiks.io.FiksProtocolRepository;
import no.difi.meldingsutveksling.serviceregistry.service.ProcessService;
import no.difi.meldingsutveksling.serviceregistry.service.brreg.BrregService;
import no.difi.meldingsutveksling.serviceregistry.service.elma.ELMALookupService;
import no.difi.meldingsutveksling.serviceregistry.service.krr.KrrService;
import no.difi.meldingsutveksling.serviceregistry.service.virksert.VirkSertService;
import no.difi.meldingsutveksling.serviceregistry.svarut.SvarUtClientException;
import no.difi.meldingsutveksling.serviceregistry.svarut.SvarUtService;
import no.difi.move.common.oauth.KeystoreHelper;
import no.difi.vefa.peppol.common.model.ProcessIdentifier;
import no.difi.vefa.peppol.lookup.LookupClient;
import no.difi.virksert.client.lang.VirksertClientException;
import no.ks.fiks.io.client.FiksIOKlient;
import no.ks.fiks.io.client.model.FiksOrgId;
import no.ks.fiks.io.client.model.Konto;
import no.ks.fiks.io.client.model.KontoId;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.ws.transport.http.AbstractHttpWebServiceMessageSender;
import org.springframework.ws.transport.http.HttpComponentsMessageSender;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest
@TestPropertySource("classpath:application-test.properties")
public class ServiceRecordServiceTest {

    @Autowired
    private ServiceRecordService service;

    @MockBean
    private VirkSertService virkSertService;

    @MockBean
    private ELMALookupService lookupService;

    @MockBean
    private KrrService krrService;

    @MockBean
    private BrregService brregService;

    @MockBean
    private SvarUtService svarUtService;

    @MockBean
    private ProcessService processService;

    @MockBean
    private KeystoreHelper keystoreHelper;

    @MockBean
    private HttpComponentsMessageSender httpComponentsMessageSender;

    @MockBean
    private SRRequestScope requestScope;

    @MockBean
    private LookupClient lookupClient;

    @MockBean(name = "svarUtMessageSender")
    private AbstractHttpWebServiceMessageSender messageSender;

    @MockBean
    private ServiceregistryProperties props;

    @MockBean
    private FiksIOKlient fiksIOKlient;

    @MockBean
    private FiksIoService fiksIoService;

    @MockBean
    private FiksProtocolRepository fiksProtocolRepository;

    private static String ORGNR = "123456789";
    private static String ORGNR_FIKS = "987654321";
    private static String ORGNR_EINNSYN_JOURNALPOST = "123123123";
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
    private static String EINNSYN_PROCESS_RESPONSE = "urn:no:difi:profile:einnsyn:response:ver1.0";
    private static String EINNSYN_DOCTYPE_RESPONSE_KVITTERING = "urn:no:difi:einnsyn:xsd::einnsyn_kvittering";
    private static String EINNSYN_DOCTYPE_RESPONSE_STATUS = "urn:no:difi:eformidling:xsd::status";

    private static String DIGITALPOST_PROCESS_VEDTAK = "urn:no:difi:profile:digitalpost:vedtak:ver1.0";
    private static String DIGITALPOST_DOCTYPE_PRINT = "urn:no:difi:digitalpost:xsd:fysisk::print";

    private static Process processAdmin;
    private static Process processAvtalt;
    private static Process processSkatt;
    private static Process einnsynJournalpostProcess;
    private static Process einnsynInnsynskravProcess;

    @Before
    public void init() throws MalformedURLException {
        ServiceregistryProperties.FeatureToggle feature = new ServiceregistryProperties.FeatureToggle();
        feature.setEnableDpfDpv(true);
        when(props.getFeature()).thenReturn(feature);

        ServiceregistryProperties.Altinn dpoConfig = new ServiceregistryProperties.Altinn();
        dpoConfig.setEndpointURL(new URL("http://test"));
        dpoConfig.setServiceCode("1234");
        dpoConfig.setServiceEditionCode("123456");
        when(props.getDpo()).thenReturn(dpoConfig);

        ServiceregistryProperties.PostVirksomhet dpvConfig = new ServiceregistryProperties.PostVirksomhet();
        dpvConfig.setEndpointURL(new URL("http://foo"));
        when(props.getDpv()).thenReturn(dpvConfig);

        ServiceregistryProperties.SvarUt svarUtConfig = new ServiceregistryProperties.SvarUt();
        svarUtConfig.setCertificate(new ByteArrayResource("cert1234".getBytes()));
        svarUtConfig.setServiceRecordUrl(new URL("http://foo"));
        svarUtConfig.setUser("foo");
        svarUtConfig.setPassword("bar");

        ServiceregistryProperties.Fiks fiks = new ServiceregistryProperties.Fiks();
        fiks.setSvarut(svarUtConfig);
        ServiceregistryProperties.FiksIo fiksIo = new ServiceregistryProperties.FiksIo();
        fiksIo.setOrgformFilter(Collections.singletonList("KOMM"));
        fiksIo.setEnable(true);
        fiks.setIo(fiksIo);
        when(props.getFiks()).thenReturn(fiks);

        ServiceregistryProperties.DigitalPostInnbygger dpiProps = new ServiceregistryProperties.DigitalPostInnbygger();
        dpiProps.setVedtakProcess(DIGITALPOST_PROCESS_VEDTAK);
        when(props.getDpi()).thenReturn(dpiProps);

        ServiceregistryProperties.ELMA elmaProps = new ServiceregistryProperties.ELMA();
        elmaProps.setLookupIcd(ELMA_LOOKUP_ICD);
        when(props.getElma()).thenReturn(elmaProps);


        //Prosess og dokumenttype Arkivmelding
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
        when(processService.findAll(ProcessCategory.ARKIVMELDING)).thenReturn(Sets.newHashSet(processAdmin, processSkatt));
        when(processService.getDefaultArkivmeldingProcess()).thenReturn(processAdmin);
        when(lookupService.lookupRegisteredProcesses(eq(String.format("%s:%s", ELMA_LOOKUP_ICD, ORGNR_FIKS)), anySet())).thenReturn(Sets.newHashSet());

        //Prosess og dokumenttype Avtalt
        DocumentType documentTypeAvtalt = new DocumentType()
                .setIdentifier(AVTALT_DOCTYPE);
        processAvtalt = new Process().setIdentifier(AVTALT_PROCESS)
                .setCategory(ProcessCategory.AVTALT)
                .setServiceCode("4192")
                .setServiceEditionCode("270815");
        processAvtalt.setDocumentTypes(Lists.newArrayList(documentTypeAvtalt));
        documentTypeAvtalt.setProcesses(Lists.newArrayList(processAvtalt));
        when(processService.findAll(ProcessCategory.AVTALT)).thenReturn(Sets.newHashSet(processAvtalt));
        when(processService.getDefaultArkivmeldingProcess()).thenReturn(processAvtalt);
        when(lookupService.lookupRegisteredProcesses(eq(String.format("%s:%s", ELMA_LOOKUP_ICD, ORGNR)), anySet())).thenReturn(Sets.newHashSet());


        Process vedtakProcess = new Process()
                .setIdentifier(DIGITALPOST_PROCESS_VEDTAK)
                .setCategory(ProcessCategory.DIGITALPOST)
                .setDocumentTypes(Lists.newArrayList(new DocumentType().setIdentifier(DIGITALPOST_DOCTYPE_PRINT)));
        when(processService.findAll(ProcessCategory.DIGITALPOST)).thenReturn(Sets.newHashSet(vedtakProcess));

        DocumentType einnsynJournalpostDocumentType = new DocumentType()
                .setIdentifier(EINNSYN_DOCTYPE_JOURNALPOST);
        einnsynJournalpostProcess = new Process()
                .setIdentifier(EINNSYN_PROCESS_JOURNALPOST)
                .setCategory(ProcessCategory.EINNSYN)
                .setServiceCode("data")
                .setDocumentTypes(Lists.newArrayList(einnsynJournalpostDocumentType));
        einnsynJournalpostDocumentType.setProcesses(Lists.newArrayList(einnsynJournalpostProcess));
        when(processService.findByIdentifier(EINNSYN_PROCESS_JOURNALPOST)).thenReturn(Optional.of(einnsynJournalpostProcess));

        DocumentType einnsynInnsynskravDocumentType = new DocumentType()
                .setIdentifier(EINNSYN_DOCTYPE_INNSYNSKRAV);
        einnsynInnsynskravProcess = new Process()
                .setIdentifier(EINNSYN_PROCESS_INNSYNSKRAV)
                .setCategory(ProcessCategory.EINNSYN)
                .setServiceCode("innsyn")
                .setDocumentTypes(Lists.newArrayList(einnsynInnsynskravDocumentType));
        einnsynInnsynskravDocumentType.setProcesses(Lists.newArrayList(einnsynInnsynskravProcess));
        when(processService.findByIdentifier(EINNSYN_PROCESS_INNSYNSKRAV)).thenReturn(Optional.of(einnsynInnsynskravProcess));

        when(processService.findAll(ProcessCategory.EINNSYN)).thenReturn(Sets.newHashSet(einnsynJournalpostProcess, einnsynInnsynskravProcess));

        when(lookupService.lookupRegisteredProcesses(eq(String.format("%s:%s", ELMA_LOOKUP_ICD, ORGNR_EINNSYN_JOURNALPOST)), anySet())).thenReturn(Sets.newHashSet());
        when(lookupService.lookupRegisteredProcesses(eq(String.format("%s:%s", ELMA_LOOKUP_ICD, ORGNR_EINNSYN_JOURNALPOST)), anySet()))
                .thenReturn(Sets.newHashSet(ProcessIdentifier.of(EINNSYN_PROCESS_JOURNALPOST)));

    }

    @Test(expected = SecurityLevelNotFoundException.class)
    public void createArkivmeldingServiceRecord_IdentifierHasSvarUtRegistrationOnDifferentSecurityLevel_ShouldThrowDedicatedException() throws SecurityLevelNotFoundException, CertificateNotFoundException, SvarUtClientException {
        when(processService.findByIdentifier(anyString())).thenReturn(Optional.of(mock(Process.class)));
        when(svarUtService.hasSvarUtAdressering(anyString(), any())).thenReturn(Optional.empty());
        service.createArkivmeldingServiceRecord(ORGNR_ORG, mock(Process.class), 4);
    }

    @Test
    public void createArkivMeldingServiceRecord_OrganizationHasAdministrationInSmp_ShouldReturnDpoServiceRecord() throws SecurityLevelNotFoundException, CertificateNotFoundException, SvarUtClientException, VirksertClientException {
        Process processMock = mock(Process.class);
        when(processMock.getIdentifier()).thenReturn(ARKIVMELDING_PROCESS_ADMIN);
        when(processService.findByIdentifier(anyString())).thenReturn(Optional.of(processMock));
        when(virkSertService.getCertificate(anyString())).thenReturn("pem123");
        setupLookupServiceMockToReturnAdministrationProcessMatch();

        Optional<ServiceRecord> result = service.createArkivmeldingServiceRecord(ORGNR_ORG, processAdmin, null);

        assertTrue(result.isPresent());
        assertEquals(ServiceIdentifier.DPO, result.get().getService().getIdentifier());
    }

    @Test
    public void createAvtaltServiceRecord_OrganizationHasAvtaltInSmp_ShouldReturnDpoServiceRecord() throws CertificateNotFoundException, VirksertClientException {
        Process processMock = mock(Process.class);
        when(processMock.getIdentifier()).thenReturn(AVTALT_PROCESS);
        when(processMock.getCategory()).thenReturn(ProcessCategory.AVTALT);
        when(processService.findByIdentifier(anyString())).thenReturn(Optional.of(processMock));
        when(virkSertService.getCertificate(anyString())).thenReturn("pem123");

        when(lookupService.lookupRegisteredProcesses(eq(String.format("%s:%s", ELMA_LOOKUP_ICD, ORGNR)), anySet()))
                .thenReturn(Sets.newHashSet(ProcessIdentifier.of(AVTALT_PROCESS)));

        Optional<ServiceRecord> result = service.createServiceRecord(new OrganizationInfo(ORGNR, ORGL), processAvtalt, null);

        assertTrue(result.isPresent());
        assertEquals(ServiceIdentifier.DPO, result.get().getService().getIdentifier());
    }

    @Test(expected = CertificateNotFoundException.class)
    public void createArkivMeldingServiceRecord_CertificateMissingForSmpProcess_ShouldThrowDedicatedException() throws SecurityLevelNotFoundException, VirksertClientException, CertificateNotFoundException, SvarUtClientException {
        Process processMock = mock(Process.class);
        when(processMock.getIdentifier()).thenReturn(ARKIVMELDING_PROCESS_ADMIN);
        when(processService.findByIdentifier(anyString())).thenReturn(Optional.of(processMock));
        setupLookupServiceMockToReturnAdministrationProcessMatch();
        when(virkSertService.getCertificate(anyString())).thenThrow(new VirksertClientException("certificate not found"));

        service.createArkivmeldingServiceRecord(ORGNR_ORG, processAdmin, null);
    }

    @Test
    public void createArkivmeldingServiceRecord_OrganizationHasSvarUtRegistration_ShouldReturnDpfServiceRecord() throws SecurityLevelNotFoundException, CertificateNotFoundException, SvarUtClientException {
        when(processService.findByIdentifier(anyString())).thenReturn(Optional.of(mock(Process.class)));
        when(svarUtService.hasSvarUtAdressering(eq(ORGNR_FIKS), eq(4))).thenReturn(Optional.of(4));

        Optional<ServiceRecord> result = service.createArkivmeldingServiceRecord(ORGNR_FIKS_KOMM, processSkatt, 4);

        assertTrue(result.isPresent());
        assertEquals(ServiceIdentifier.DPF, result.get().getService().getIdentifier());
    }

    @Test
    public void createArkivmeldingServiceRecord_OrganizationHasNoSkattInSmpOrSvarutRegistration_ShouldReturnDpvServiceRecord() throws SecurityLevelNotFoundException, CertificateNotFoundException, SvarUtClientException {
        when(processService.findByIdentifier(anyString())).thenReturn(Optional.of(mock(Process.class)));
        when(svarUtService.hasSvarUtAdressering(anyString(), any())).thenReturn(Optional.empty());

        Optional<ServiceRecord> result = service.createArkivmeldingServiceRecord(ORGNR_ORG, processSkatt, null);

        assertTrue(result.isPresent());
        assertEquals(ServiceIdentifier.DPV, result.get().getService().getIdentifier());
    }

    @Test
    public void createArkivmeldingServiceRecord_NoSmpNorSvarUtRegistration_ShouldReturnDpvServiceRecord() throws SecurityLevelNotFoundException, CertificateNotFoundException, SvarUtClientException {
        when(processService.findByIdentifier(anyString())).thenReturn(Optional.of(mock(Process.class)));
        when(lookupService.lookup(anyString(), anySet())).thenReturn(Lists.newArrayList());
        when(svarUtService.hasSvarUtAdressering(anyString(), any())).thenReturn(Optional.empty());

        Optional<ServiceRecord> result = service.createArkivmeldingServiceRecord(ORGNR_ORG, processSkatt, null);

        assertTrue(result.isPresent());
        assertEquals(ServiceIdentifier.DPV, result.get().getService().getIdentifier());
    }

    private void setupLookupServiceMockToReturnAdministrationProcessMatch() {
        when(lookupService.lookupRegisteredProcesses(eq(String.format("%s:%s", ELMA_LOOKUP_ICD, ORGNR)), anySet()))
                .thenReturn(Sets.newHashSet(ProcessIdentifier.of(ARKIVMELDING_PROCESS_ADMIN)));
    }

    @Test
    public void createArkivmeldingServiceRecords_NoProcessesFound_ShouldReturnEmptyList() throws SecurityLevelNotFoundException, CertificateNotFoundException, SvarUtClientException {
        when(processService.findAll(ProcessCategory.ARKIVMELDING)).thenReturn(Sets.newHashSet());
        when(svarUtService.hasSvarUtAdressering(anyString(), any())).thenReturn(Optional.empty());

        List<ServiceRecord> result = service.createArkivmeldingServiceRecords(new OrganizationInfo(), null);

        assertTrue(result.isEmpty());
    }

    @Test(expected = CertificateNotFoundException.class)
    public void createArkivmeldingServiceRecords_CertificateNotFoundForSmpProcess_ShouldThrowDedicatedException() throws SecurityLevelNotFoundException, CertificateNotFoundException, VirksertClientException, SvarUtClientException {
        setupLookupServiceMockToReturnAdministrationProcessMatch();
        when(virkSertService.getCertificate(anyString())).thenThrow(new VirksertClientException("certificate not found"));
        service.createArkivmeldingServiceRecords(ORGNR_ORG, null);
    }

    @Test
    public void createArkivmeldingServiceRecords_OrganizationHasAdministrasjonButNotSkattRegistrationInSmp_ShouldReturnCorrespondingDpoAndDpvServiceRecords() throws SecurityLevelNotFoundException, CertificateNotFoundException, SvarUtClientException, VirksertClientException {
        setupLookupServiceMockToReturnAdministrationProcessMatch();
        when(virkSertService.getCertificate(anyString())).thenReturn("pem123");

        List<ServiceRecord> result = service.createArkivmeldingServiceRecords(ORGNR_ORG, null);

        assertEquals(2, result.size());
        ServiceRecord srAdmin = result.stream().filter(r -> ARKIVMELDING_PROCESS_ADMIN.equals(r.getProcess())).findFirst().orElseThrow(RuntimeException::new);
        assertEquals(ServiceIdentifier.DPO, srAdmin.getService().getIdentifier());
        ServiceRecord srSkatt = result.stream().filter(r -> ARKIVMELDING_PROCESS_SKATT.equals(r.getProcess())).findFirst().orElseThrow(RuntimeException::new);
        assertEquals(ServiceIdentifier.DPV, srSkatt.getService().getIdentifier());
    }

    @Test
    public void createArkivmeldingServiceRecords_OrganizationHasSvarUtRegistration_ShouldReturnDpfServiceRecord() throws SecurityLevelNotFoundException, CertificateNotFoundException, SvarUtClientException {
        when(svarUtService.hasSvarUtAdressering(eq(ORGNR_FIKS), any())).thenReturn(Optional.of(3));
        List<ServiceRecord> result = service.createArkivmeldingServiceRecords(ORGNR_FIKS_KOMM, 3);
        assertEquals(2, countServiceRecordsForServiceIdentifier(result, ServiceIdentifier.DPF));
    }

    @Test
    public void createArkivmeldingServiceRecords_OrganizationHasNoSmpNorSvarutRegistration_ShouldReturnDpvServiceRecord() throws SecurityLevelNotFoundException, CertificateNotFoundException, SvarUtClientException {
        when(lookupService.lookup(anyString(), anySet())).thenReturn(Lists.newArrayList());
        when(svarUtService.hasSvarUtAdressering(anyString(), any())).thenReturn(Optional.empty());

        List<ServiceRecord> result = service.createArkivmeldingServiceRecords(ORGNR_ORG, null);

        assertEquals(2, countServiceRecordsForServiceIdentifier(result, ServiceIdentifier.DPV));
    }

    @Test(expected = SecurityLevelNotFoundException.class)
    public void createArkivmeldingServiceRecords_IdentifierHasSvarUtRegistrationOnDifferentSecurityLevel_ShouldThrowDedicatedException
            () throws SecurityLevelNotFoundException, CertificateNotFoundException, SvarUtClientException {
        when(svarUtService.hasSvarUtAdressering(eq(ORGNR_FIKS), any())).thenReturn(Optional.empty());
        service.createArkivmeldingServiceRecords(ORGNR_FIKS_KOMM, 4);
    }

    @Test(expected = SvarUtClientException.class)
    public void createArkivmeldingServiceRecords_SvarUtServiceIsUnavailable_ShouldReturnEmpty() throws
            SvarUtClientException, SecurityLevelNotFoundException, CertificateNotFoundException {
        when(svarUtService.hasSvarUtAdressering(eq(ORGNR_FIKS), any()))
                .thenThrow(new SvarUtClientException(new RuntimeException("service unavailable")));
        service.createArkivmeldingServiceRecords(ORGNR_FIKS_KOMM, 3);
    }

    private long countServiceRecordsForServiceIdentifier(List<ServiceRecord> result, ServiceIdentifier
            serviceIdentifier) {
        return result.stream().filter(serviceRecord -> serviceIdentifier == serviceRecord.getService().getIdentifier()).count();
    }

    @Test
    public void createEinnsynServiceRecords_ShouldReturnDpeServiceRecord() throws CertificateNotFoundException, VirksertClientException {
        when(virkSertService.getCertificate(anyString())).thenReturn("pem123");
        List<ServiceRecord> result = service.createEinnsynServiceRecords(new OrganizationInfo().setIdentifier(ORGNR_EINNSYN_JOURNALPOST), 3);
        assertEquals(1, result.size());
        ServiceRecord journalpostServiceRecord = result.stream().filter(r -> EINNSYN_PROCESS_JOURNALPOST.equals(r.getProcess())).findFirst().orElseThrow(RuntimeException::new);
        assertEquals(ServiceIdentifier.DPE, journalpostServiceRecord.getService().getIdentifier());
    }

    @Test
    public void createEinnsynServiceRecords_OrgnrNotInElma_ShouldNotReturnDpeServiceRecord() throws CertificateNotFoundException {
        when(lookupService.lookup(eq(String.format("%s:%s", ELMA_LOOKUP_ICD, ORGNR_EINNSYN_RESPONSE)), anySet())).thenReturn(Lists.newArrayList());
        List<ServiceRecord> result = service.createEinnsynServiceRecords(new OrganizationInfo().setIdentifier(ORGNR_EINNSYN_RESPONSE).setOrganizationType(ORGL), 3);
        assertTrue(result.isEmpty());
    }

    @Test
    public void createEinnsynServiceRecords_EndpointurlNotFound_ShouldNotReturnDpeServiceRecord() throws
            CertificateNotFoundException {
        List<ServiceRecord> result = service.createEinnsynServiceRecords(new OrganizationInfo().setIdentifier(ORGNR_EINNSYN).setOrganizationType(ORGL), 3);
        assertTrue(result.isEmpty());
    }

    @Test
    public void createEinnsynServiceRecord_HasOrgnrAndProcessidentifier_ShouldReturnDpeServiceRecord() throws CertificateNotFoundException, VirksertClientException {
        when(virkSertService.getCertificate(anyString())).thenReturn("pem123");
        Optional<ServiceRecord> result = service.createServiceRecord(new OrganizationInfo().setIdentifier(ORGNR_EINNSYN_JOURNALPOST), einnsynJournalpostProcess, 3);
        assertTrue(result.isPresent());
        assertEquals(ServiceIdentifier.DPE, result.get().getService().getIdentifier());
    }

    @Test
    public void createEinnsynServiceRecord_HasOrgnrWhileProcessidentifierMatchNotFoundInElma_ShouldNotReturnDpeServiceRecord
            () throws CertificateNotFoundException, ProcessNotFoundException {
        DocumentType einnsynResponseDocumentType = new DocumentType()
                .setIdentifier(EINNSYN_DOCTYPE_RESPONSE_KVITTERING)
                .setIdentifier(EINNSYN_DOCTYPE_RESPONSE_STATUS);
        Process einnsynResponseProcess = new Process()
                .setIdentifier(EINNSYN_PROCESS_RESPONSE)
                .setCategory(ProcessCategory.EINNSYN)
                .setServiceCode("567")
                .setServiceEditionCode("5678");
        Optional<Process> responseProcess = Optional.of(einnsynResponseProcess);
        einnsynResponseDocumentType.setProcesses(Lists.newArrayList(einnsynResponseProcess));
        einnsynResponseProcess.setDocumentTypes(Lists.newArrayList(einnsynResponseDocumentType));
        when(processService.findByIdentifier(EINNSYN_PROCESS_RESPONSE)).thenReturn(responseProcess);
        when(lookupService.lookupRegisteredProcesses(eq(String.format("%s:%s", ELMA_LOOKUP_ICD, ORGNR_EINNSYN_RESPONSE)), anySet()))
                .thenReturn(Sets.newHashSet());

        Optional<ServiceRecord> result = service.createServiceRecord(new OrganizationInfo().setIdentifier(ORGNR_EINNSYN_RESPONSE).setOrganizationType(ORGL), einnsynResponseProcess, 3);
        assertFalse(result.isPresent());
    }

    @Test
    public void createEinnsynServiceRecord_HasFiksIoKonto_ShouldReturnDpfioServiceRecord() throws CertificateNotFoundException, ProcessNotFoundException {
        String kommOrgnr = "111222333";
        when(lookupService.lookupRegisteredProcesses(eq(String.format("%s:%s", ELMA_LOOKUP_ICD, kommOrgnr)), anySet())).thenReturn(Sets.newHashSet());
        String kontoId = "10b58f58-3d8c-46d4-b17e-439ac66c79fc";
        Konto konto = Konto.builder()
                .kontoId(new KontoId(UUID.fromString(kontoId)))
                .kontoNavn("Testkommune")
                .fiksOrgId(new FiksOrgId(UUID.fromString("55e8572a-7515-4518-aa37-e20029a78739")))
                .fiksOrgNavn("Testorg")
                .build();
        when(fiksIoService.lookup(any(), any(), anyInt())).thenReturn(Optional.of(konto));
        when(fiksProtocolRepository.findByProcessesIdentifier(einnsynInnsynskravProcess.getIdentifier()))
                .thenReturn(new FiksProtocol(null, "proto.test", Sets.newHashSet(einnsynInnsynskravProcess)));

        OrganizationInfo kommOrg = new OrganizationInfo(kommOrgnr, new OrganizationType("KOMM"));
        Optional<ServiceRecord> serviceRecord = service.createServiceRecord(kommOrg, einnsynInnsynskravProcess, 3);
        assertTrue(serviceRecord.isPresent());
        assertEquals(ServiceIdentifier.DPFIO, serviceRecord.get().getService().getIdentifier());
        assertEquals(kontoId, serviceRecord.get().getService().getEndpointUrl());
    }

}

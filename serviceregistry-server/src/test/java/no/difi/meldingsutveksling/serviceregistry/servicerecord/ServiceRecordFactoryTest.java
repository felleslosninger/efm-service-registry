package no.difi.meldingsutveksling.serviceregistry.servicerecord;

import no.difi.meldingsutveksling.serviceregistry.CertificateNotFoundException;
import no.difi.meldingsutveksling.serviceregistry.config.ServiceregistryProperties;
import no.difi.meldingsutveksling.serviceregistry.exceptions.EndpointUrlNotFound;
import no.difi.meldingsutveksling.serviceregistry.exceptions.SecurityLevelNotFoundException;
import no.difi.meldingsutveksling.serviceregistry.krr.DsfLookupException;
import no.difi.meldingsutveksling.serviceregistry.krr.KRRClientException;
import no.difi.meldingsutveksling.serviceregistry.krr.LookupParameters;
import no.difi.meldingsutveksling.serviceregistry.krr.PersonResource;
import no.difi.meldingsutveksling.serviceregistry.model.DocumentType;
import no.difi.meldingsutveksling.serviceregistry.model.Process;
import no.difi.meldingsutveksling.serviceregistry.model.ProcessCategory;
import no.difi.meldingsutveksling.serviceregistry.model.ServiceIdentifier;
import no.difi.meldingsutveksling.serviceregistry.service.ProcessService;
import no.difi.meldingsutveksling.serviceregistry.service.brreg.BrregService;
import no.difi.meldingsutveksling.serviceregistry.service.elma.ELMALookupService;
import no.difi.meldingsutveksling.serviceregistry.service.krr.KrrService;
import no.difi.meldingsutveksling.serviceregistry.service.virksert.VirkSertService;
import no.difi.meldingsutveksling.serviceregistry.svarut.SvarUtService;
import no.difi.move.common.oauth.KeystoreHelper;
import no.difi.vefa.peppol.common.model.*;
import no.difi.virksert.client.lang.VirksertClientException;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetails;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.ws.transport.http.HttpComponentsMessageSender;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ServiceRecordFactoryTest {

    @Autowired
    private ServiceRecordFactory factory;

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
    private ServiceregistryProperties props;

    private static String ORGNR = "123456789";
    private static String ORGNR_FIKS = "987654321";
    private static String ORGNR_EINNSYN_JOURNALPOST = "123123123";
    private static String ORGNR_EINNSYN_RESPONSE = "987987987";
    private static String ORGNR_EINNSYN = "123987654";
    private static String ARKIVMELDING_DOCTYPE = "urn:no:difi:arkivmelding:xsd::arkivmelding";
    private static String ARKIVMELDING_PROCESS_ADMIN = "urn:no:difi:profile:arkivmelding:administrasjon:ver1.0";
    private static String ARKIVMELDING_PROCESS_SKATT = "urn:no:difi:profile:arkivmelding:skatterOgAvgifter:ver1.0";
    private static String EINNSYN_PROCESS_JOURNALPOST = "urn:no:difi:profile:einnsyn:journalpost:ver1.0";
    private static String EINNSYN_DOCTYPE_JOURNALPOST = "urn:no:difi:einnsyn:xsd::publisering";
    private static String EINNSYN_PROCESS_RESPONSE = "urn:no:difi:profile:einnsyn:response:ver1.0";
    private static String EINNSYN_DOCTYPE_RESPONSE_KVITTERING = "urn:no:difi:einnsyn:xsd::einnsyn_kvittering";
    private static String EINNSYN_DOCTYPE_RESPONSE_STATUS = "urn:no:difi:eformidling:xsd::status";
    private static String PERSONNUMMER = "01234567890";

    @Before
    public void init() throws MalformedURLException, EndpointUrlNotFound {
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
        when(props.getSvarut()).thenReturn(svarUtConfig);
        DocumentType documentType = new DocumentType()
                .setIdentifier(ARKIVMELDING_DOCTYPE);
        Process processAdmin = new Process()
                .setIdentifier(ARKIVMELDING_PROCESS_ADMIN)
                .setCategory(ProcessCategory.ARKIVMELDING)
                .setServiceCode("4192")
                .setServiceEditionCode("270815");
        Process processSkatt = new Process()
                .setIdentifier(ARKIVMELDING_PROCESS_SKATT)
                .setCategory(ProcessCategory.ARKIVMELDING)
                .setServiceCode("4192")
                .setServiceEditionCode("270815");
        processSkatt.setDocumentTypes(Lists.newArrayList(documentType));
        processAdmin.setDocumentTypes(Lists.newArrayList(documentType));
        documentType.setProcesses(Lists.newArrayList(processAdmin, processSkatt));
        when(processService.findAll(ProcessCategory.ARKIVMELDING)).thenReturn(Lists.newArrayList(processAdmin, processSkatt));
        when(lookupService.lookup(Matchers.eq("9908:" + ORGNR_FIKS), any(List.class))).thenReturn(Lists.newArrayList());

        DocumentType einnsynJournalpostDocumentType = new DocumentType()
                .setIdentifier(EINNSYN_DOCTYPE_JOURNALPOST);
        Process einnsynJournalpostProcess = new Process()
                .setIdentifier(EINNSYN_PROCESS_JOURNALPOST)
                .setCategory(ProcessCategory.EINNSYN)
                .setServiceCode("567")
                .setServiceEditionCode("5678");
        Optional<Process> journalpostProcess = Optional.of(einnsynJournalpostProcess);
        einnsynJournalpostProcess.setDocumentTypes(Lists.newArrayList(einnsynJournalpostDocumentType));
        einnsynJournalpostDocumentType.setProcesses(Lists.newArrayList(einnsynJournalpostProcess));
        when(processService.findAll(ProcessCategory.EINNSYN)).thenReturn(Lists.newArrayList(einnsynJournalpostProcess));
        when(lookupService.lookup(Matchers.eq("9908:" + ORGNR_EINNSYN_JOURNALPOST), any(List.class))).thenReturn(Lists.newArrayList());
        when(processService.findByIdentifier(EINNSYN_PROCESS_JOURNALPOST)).thenReturn(journalpostProcess);
        ProcessMetadata<Endpoint> einnsynProcessMetadata =
                ProcessMetadata.of(ProcessIdentifier.of(EINNSYN_PROCESS_JOURNALPOST), Endpoint.of(null, null, null));
        ServiceMetadata einnsynServiceMetadata =
                ServiceMetadata.of(ParticipantIdentifier.of("9908:" + ORGNR_EINNSYN_JOURNALPOST),
                        DocumentTypeIdentifier.of(EINNSYN_DOCTYPE_JOURNALPOST),
                        Arrays.asList(einnsynProcessMetadata));
        when(lookupService.lookup(Matchers.eq("9908:" + ORGNR_EINNSYN_JOURNALPOST), any(List.class))).thenReturn(Lists.newArrayList(einnsynServiceMetadata));

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
        ProcessMetadata<Endpoint> einnsynResponseProcessMetadata =
                ProcessMetadata.of(ProcessIdentifier.of(EINNSYN_PROCESS_RESPONSE), Endpoint.of(null, null, null));
        ServiceMetadata.of(ParticipantIdentifier.of("9908:" + ORGNR_EINNSYN_RESPONSE),
                DocumentTypeIdentifier.of(EINNSYN_DOCTYPE_RESPONSE_KVITTERING),
                Arrays.asList(einnsynResponseProcessMetadata));
        when(lookupService.lookup(Matchers.eq("9908:" + ORGNR_EINNSYN_RESPONSE), any(List.class))).thenReturn(Lists.newArrayList());
        when(processService.findByIdentifier(EINNSYN_PROCESS_RESPONSE)).thenReturn(responseProcess);
        when(lookupService.lookup(Matchers.eq("9908:" + ORGNR_EINNSYN), any(List.class))).thenThrow(new RuntimeException("Endpoint Url was not found in ELMA"));


    }

    @Test
    public void createArkivmeldingServiceRecord_ProcessIsNotFound_ShouldReturnNotFound() throws SecurityLevelNotFoundException, CertificateNotFoundException {
        when(processService.findByIdentifier(anyString())).thenReturn(Optional.empty());
        Optional<ServiceRecord> result = factory.createArkivmeldingServiceRecord(ORGNR, "NotFound", null);
        assertFalse(result.isPresent());
    }

    @Test(expected = SecurityLevelNotFoundException.class)
    public void createArkivmeldingServiceRecord_IdentifierHasSvarUtRegistrationOnDifferentSecurityLevel_ShouldThrowDedicatedException() throws SecurityLevelNotFoundException, CertificateNotFoundException {
        when(processService.findByIdentifier(anyString())).thenReturn(Optional.of(mock(Process.class)));
        when(svarUtService.hasSvarUtAdressering(anyString(), any())).thenReturn(Optional.empty());
        factory.createArkivmeldingServiceRecord(ORGNR, "Found", 3);
    }

    @Test
    public void createArkivMeldingServiceRecord_OrganizationHasAdministrationInSmp_ShouldReturnDpoServiceRecord() throws EndpointUrlNotFound, SecurityLevelNotFoundException, CertificateNotFoundException {
        when(processService.findByIdentifier(anyString())).thenReturn(Optional.of(mock(Process.class)));
        setupLookupServiceMockToReturnAdministrationProcessMatch();

        Optional<ServiceRecord> result = factory.createArkivmeldingServiceRecord(ORGNR, ARKIVMELDING_PROCESS_ADMIN, null);

        assertTrue(result.isPresent());
        assertEquals(ServiceIdentifier.DPO, result.get().getService().getIdentifier());
    }

    @Test(expected = CertificateNotFoundException.class)
    public void createArkivMeldingServiceRecord_CertificateMissingForSmpProcess_ShouldThrowDedicatedException() throws EndpointUrlNotFound, SecurityLevelNotFoundException, VirksertClientException, CertificateNotFoundException {
        when(processService.findByIdentifier(anyString())).thenReturn(Optional.of(mock(Process.class)));
        setupLookupServiceMockToReturnAdministrationProcessMatch();
        when(virkSertService.getCertificate(anyString())).thenThrow(new VirksertClientException("certificate not found"));

        factory.createArkivmeldingServiceRecord(ORGNR, ARKIVMELDING_PROCESS_ADMIN, null);
    }

    @Test
    public void createArkivmeldingServiceRecord_OrganizationHasSvarUtRegistration_ShouldReturnDpfServiceRecord() throws SecurityLevelNotFoundException, CertificateNotFoundException {
        when(processService.findByIdentifier(anyString())).thenReturn(Optional.of(mock(Process.class)));
        when(svarUtService.hasSvarUtAdressering(eq(ORGNR_FIKS), eq(4))).thenReturn(Optional.of(4));

        Optional<ServiceRecord> result = factory.createArkivmeldingServiceRecord(ORGNR_FIKS, ARKIVMELDING_PROCESS_SKATT, 4);

        assertTrue(result.isPresent());
        assertEquals(ServiceIdentifier.DPF, result.get().getService().getIdentifier());
    }

    @Test
    public void createArkivmeldingServiceRecord_OrganizationHasNoSkattInSmpOrSvarutRegistration_ShouldReturnDpvServiceRecord() throws SecurityLevelNotFoundException, CertificateNotFoundException {
        when(processService.findByIdentifier(anyString())).thenReturn(Optional.of(mock(Process.class)));
        when(svarUtService.hasSvarUtAdressering(anyString(), any())).thenReturn(Optional.empty());

        Optional<ServiceRecord> result = factory.createArkivmeldingServiceRecord(ORGNR, ARKIVMELDING_PROCESS_SKATT, null);

        assertTrue(result.isPresent());
        assertEquals(ServiceIdentifier.DPV, result.get().getService().getIdentifier());
    }

    @Test
    public void createArkivmeldingServiceRecord_NoSmpNorSvarUtRegistration_ShouldReturnDpvServiceRecord() throws SecurityLevelNotFoundException, EndpointUrlNotFound, CertificateNotFoundException {
        when(processService.findByIdentifier(anyString())).thenReturn(Optional.of(mock(Process.class)));
        when(lookupService.lookup(anyString(), anyList())).thenReturn(new ArrayList());
        when(svarUtService.hasSvarUtAdressering(anyString(), any())).thenReturn(Optional.empty());

        Optional<ServiceRecord> result = factory.createArkivmeldingServiceRecord(ORGNR, ARKIVMELDING_PROCESS_SKATT, null);

        assertTrue(result.isPresent());
        assertEquals(ServiceIdentifier.DPV, result.get().getService().getIdentifier());
    }

    private void setupLookupServiceMockToReturnAdministrationProcessMatch() throws EndpointUrlNotFound {
        ProcessMetadata<Endpoint> processMetadata =
                ProcessMetadata.of(ProcessIdentifier.of(ARKIVMELDING_PROCESS_ADMIN), Endpoint.of(null, null, null));
        ServiceMetadata serviceMetadata =
                ServiceMetadata.of(ParticipantIdentifier.of("9908:" + ORGNR),
                        DocumentTypeIdentifier.of(ARKIVMELDING_DOCTYPE),
                        Arrays.asList(processMetadata));
        when(lookupService.lookup(Matchers.eq("9908:" + ORGNR), any(List.class))).thenReturn(Lists.newArrayList(serviceMetadata));
    }

    @Test
    public void createArkivmeldingServiceRecords_NoProcessesFound_ShouldReturnEmptyList() throws SecurityLevelNotFoundException, CertificateNotFoundException {
        when(processService.findAll(ProcessCategory.ARKIVMELDING)).thenReturn(Lists.newArrayList());
        when(svarUtService.hasSvarUtAdressering(anyString(), any())).thenReturn(Optional.empty());

        List<ServiceRecord> result = factory.createArkivmeldingServiceRecords("identifier", null);

        assertTrue(result.isEmpty());
    }

    @Test(expected = CertificateNotFoundException.class)
    public void createArkivmeldingServiceRecords_CertificateNotFoundForSmpProcess_ShouldThrowDedicatedException() throws EndpointUrlNotFound, SecurityLevelNotFoundException, CertificateNotFoundException, VirksertClientException {
        setupLookupServiceMockToReturnAdministrationProcessMatch();
        when(virkSertService.getCertificate(anyString())).thenThrow(new VirksertClientException("certificate not found"));
        factory.createArkivmeldingServiceRecords(ORGNR, null);
    }

    @Test
    public void createArkivmeldingServiceRecords_OrganizationHasAdministrasjonButNotSkattRegistrationInSmp_ShouldReturnCorrespondingDpoAndDpvServiceRecords() throws EndpointUrlNotFound, SecurityLevelNotFoundException, CertificateNotFoundException {
        setupLookupServiceMockToReturnAdministrationProcessMatch();

        List<ServiceRecord> result = factory.createArkivmeldingServiceRecords(ORGNR, null);

        assertEquals(2, result.size());
        ServiceRecord srAdmin = result.stream().filter(r -> ARKIVMELDING_PROCESS_ADMIN.equals(r.getProcess())).findFirst().orElseThrow(RuntimeException::new);
        assertEquals(ServiceIdentifier.DPO, srAdmin.getService().getIdentifier());
        ServiceRecord srSkatt = result.stream().filter(r -> ARKIVMELDING_PROCESS_SKATT.equals(r.getProcess())).findFirst().orElseThrow(RuntimeException::new);
        assertEquals(ServiceIdentifier.DPV, srSkatt.getService().getIdentifier());
    }

    @Test
    public void createArkivmeldingServiceRecords_OrganizationHasSvarUtRegistration_ShouldReturnDpfServiceRecord() throws SecurityLevelNotFoundException, CertificateNotFoundException {
        when(svarUtService.hasSvarUtAdressering(eq(ORGNR_FIKS), any())).thenReturn(Optional.of(3));
        List<ServiceRecord> result = factory.createArkivmeldingServiceRecords(ORGNR_FIKS, 3);
        assertEquals(2, countServiceRecordsForServiceIdentifier(result, ServiceIdentifier.DPF));
    }

    @Test
    public void createArkivmeldingServiceRecords_OrganizationHasNoSmpNorSvarutRegistration_ShouldReturnDpvServiceRecord() throws EndpointUrlNotFound, SecurityLevelNotFoundException, CertificateNotFoundException {
        when(lookupService.lookup(Matchers.eq("9908:" + ORGNR), any(List.class))).thenReturn(new ArrayList());
        when(svarUtService.hasSvarUtAdressering(anyString(), any())).thenReturn(Optional.empty());

        List<ServiceRecord> result = factory.createArkivmeldingServiceRecords(ORGNR, null);

        assertEquals(2, countServiceRecordsForServiceIdentifier(result, ServiceIdentifier.DPV));
    }

    @Test(expected = SecurityLevelNotFoundException.class)
    public void createArkivmeldingServiceRecords_IdentifierHasSvarUtRegistrationOnDifferentSecurityLevel_ShouldThrowDedicatedException() throws SecurityLevelNotFoundException, CertificateNotFoundException {
        when(svarUtService.hasSvarUtAdressering(eq(ORGNR_FIKS), any())).thenReturn(Optional.empty());
        factory.createArkivmeldingServiceRecords(ORGNR_FIKS, 3);
    }

    private long countServiceRecordsForServiceIdentifier(List<ServiceRecord> result, ServiceIdentifier serviceIdentifier) {
        return result.stream().filter(serviceRecord -> serviceIdentifier == serviceRecord.getService().getIdentifier()).count();
    }


    @Test
    public void createEinnsynServiceRecord_ProcessIsNotFound_ShouldReturnNotFound() throws CertificateNotFoundException {
        when(processService.findByIdentifier(anyString())).thenReturn(Optional.empty());
        Optional<ServiceRecord> result = factory.createEinnsynServiceRecord(ORGNR, "NotFound");
        assertFalse(result.isPresent());
    }

    @Test
    public void createEinnsynServiceRecords_ShouldReturnDpeServiceRecord() throws CertificateNotFoundException {
        List<ServiceRecord> result = factory.createEinnsynServiceRecords(ORGNR_EINNSYN_JOURNALPOST);
        assertEquals(1, result.size());
        ServiceRecord journalpostServiceRecord = result.stream().filter(r -> EINNSYN_PROCESS_JOURNALPOST.equals(r.getProcess())).findFirst().orElseThrow(RuntimeException::new);
        assertEquals(ServiceIdentifier.DPE, journalpostServiceRecord.getService().getIdentifier());
    }

    @Test
    public void createEinnsynServiceRecords_OrgnrNotInElma_ShouldNotReturnDpeServiceRecord() throws CertificateNotFoundException {
        List<ServiceRecord> result = factory.createEinnsynServiceRecords(ORGNR_EINNSYN_RESPONSE);
        assertEquals(0, result.size());
    }

    @Test(expected = RuntimeException.class)
    public void createEinnsynServiceRecords_EndpointurlNotFound_ShouldNotReturnDpeServiceRecord() throws CertificateNotFoundException {
        List<ServiceRecord> result = factory.createEinnsynServiceRecords(ORGNR_EINNSYN);
        assertEquals(0, result.size());
    }

    @Test
    public void createEinnsynServiceRecord_hasOrgnrAndProcessidentifier_ShouldReturnDpeServiceRecord() throws CertificateNotFoundException {
        ServiceRecord serviceRecord = null;
        Optional<ServiceRecord> result = factory.createEinnsynServiceRecord(ORGNR_EINNSYN_JOURNALPOST, EINNSYN_PROCESS_JOURNALPOST);

        assertTrue(result.isPresent());
        serviceRecord = result.get();
        assertEquals(ServiceIdentifier.DPE, serviceRecord.getService().getIdentifier());
    }

    @Test
    public void createEinnsynServiceRecord_hasOrgnrWhileProcessidentifierMatchNotFoundInElma_ShouldNotReturnDpeServiceRecord() throws CertificateNotFoundException {
        Optional<ServiceRecord> result = factory.createEinnsynServiceRecord(ORGNR_EINNSYN_RESPONSE, EINNSYN_PROCESS_RESPONSE);
        assertEquals(Optional.empty(), result);
    }

    @Test
    public void createEinnsynServiceRecord_hasOrgnrAndProcessidentifierWhileProcessNotFound_ShouldNotReturnDpeServiceRecord() throws CertificateNotFoundException {
        Optional<ServiceRecord> result = factory.createEinnsynServiceRecord(ORGNR_EINNSYN_JOURNALPOST, EINNSYN_PROCESS_RESPONSE);
        assertEquals(Optional.empty(), result);
    }

    @Test(expected = DsfLookupException.class)
    public void createDigitalpostServiceRecords_ForcePrintMessageToRecipientNotInPopulationRegistry_ShouldThrowDedicatedException() throws KRRClientException, DsfLookupException {
        Authentication authenticationMock = mock(Authentication.class);
        OAuth2AuthenticationDetails detailsMock = mock(OAuth2AuthenticationDetails.class);
        when(detailsMock.getTokenValue()).thenReturn("TOKEN");
        when(authenticationMock.getDetails()).thenReturn(detailsMock);
        PersonResource personResourceMock = mock(PersonResource.class);
        when(personResourceMock.hasMailbox()).thenReturn(false);
        when(krrService.getCitizenInfo(any(LookupParameters.class))).thenReturn(personResourceMock);
        when(krrService.getDSFInfo(any(LookupParameters.class))).thenReturn(Optional.empty());

        factory.createDigitalpostServiceRecords(PERSONNUMMER, authenticationMock, "991825827", null, true);
    }

    // TODO add tests for digitalpost and einnsyn
}

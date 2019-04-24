package no.difi.meldingsutveksling.serviceregistry.servicerecord;

import no.difi.meldingsutveksling.serviceregistry.exceptions.EndpointUrlNotFound;
import no.difi.meldingsutveksling.serviceregistry.service.brreg.BrregService;
import no.difi.meldingsutveksling.serviceregistry.service.elma.ELMALookupService;
import no.difi.meldingsutveksling.serviceregistry.service.krr.KrrService;
import no.difi.meldingsutveksling.serviceregistry.service.virksert.VirkSertService;
import no.difi.meldingsutveksling.serviceregistry.svarut.SvarUtService;
import no.difi.vefa.peppol.common.model.Endpoint;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import java.net.URI;
import java.net.URISyntaxException;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ServiceRecordFactoryJavaTest {

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

    public static String ORGNR = "123456789";

    @Before
    public void init() throws URISyntaxException, EndpointUrlNotFound {
        Endpoint ep = mock(Endpoint.class);
        when(ep.getAddress()).thenReturn(new URI(""));
        when(lookupService.lookup(any())).thenReturn(ep);
    }

    // TODO skrive om samtlige tester til ny factoryklasse....

//    @Test
//    public void eduShouldReturnServiceRecord() throws VirksertClientException {
//        when(virkSertService.getCertificate(any())).thenReturn("pem123");
//
//        Optional<ServiceRecord> serviceRecord = factory.createEduServiceRecord(ORGNR);
//        assertTrue(serviceRecord.isPresent());
//        ServiceRecord record = serviceRecord.get();
//        assertTrue(record instanceof ArkivmeldingServiceRecord);
//        assertEquals(ORGNR, record.getOrganisationNumber());
//        assertTrue(!isNullOrEmpty(record.getPemCertificate()));
//    }
//
//    @Test
//    public void eduShouldReturnEmptyRecord() throws EndpointUrlNotFound {
//        when(lookupService.lookup(any())).thenThrow(new EndpointUrlNotFound("not found in ELMA", mock(PeppolException.class)));
//        assertTrue(!factory.createEduServiceRecord(ORGNR).isPresent());
//    }
//
//    @Test
//    public void eduShouldReturnErrorRecord() throws VirksertClientException {
//        when(virkSertService.getCertificate(any())).thenThrow(new CertificateNotFoundException("", mock(VirksertClientException.class)));
//
//        Optional<ServiceRecord> serviceRecord = factory.createEduServiceRecord(ORGNR);
//        assertTrue(serviceRecord.isPresent());
//        ServiceRecord record = serviceRecord.get();
//        assertTrue(record instanceof ErrorServiceRecord);
//        assertEquals(DPO, record.getServiceIdentifier());
//    }
//
//    @Test
//    public void fiksShouldReturnServiceRecord() {
//        when(svarUtService.hasSvarUtAdressering(ORGNR)).thenReturn(Optional.of(3));
//
//        Optional<FiksWrapper> serviceRecord = factory.createFiksServiceRecord(ORGNR);
//        assertTrue(serviceRecord.isPresent());
//        ServiceRecord record = serviceRecord.get().getServiceRecord();
//        assertTrue(record instanceof FiksServiceRecord);
//        assertEquals(ORGNR, record.getOrganisationNumber());
//        assertTrue(!isNullOrEmpty(record.getPemCertificate()));
//    }
//
//    @Test
//    public void fiksShouldReturnEmptyRecord() {
//        when(svarUtService.hasSvarUtAdressering(ORGNR)).thenReturn(Optional.empty());
//        assertTrue(!factory.createFiksServiceRecord(ORGNR).isPresent());
//    }
//
//    @Test
//    public void fiksShouldReturnErrorRecord() {
//        when(svarUtService.hasSvarUtAdressering(any())).thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));
//
//        Optional<FiksWrapper> serviceRecord = factory.createFiksServiceRecord(ORGNR);
//        assertTrue(serviceRecord.isPresent());
//        ServiceRecord record = serviceRecord.get().getServiceRecord();
//        assertTrue(record instanceof ErrorServiceRecord);
//        assertEquals(DPF, record.getServiceIdentifier());
//    }
//
//    @Test
//    public void dpeShouldReturnServiceRecord() throws VirksertClientException {
//        when(lookupService.identifierHasInnsynskravCapability(any())).thenReturn(true);
//        when(virkSertService.getCertificate(any())).thenReturn("pem123");
//
//        Optional<ServiceRecord> serviceRecord = factory.createDpeInnsynServiceRecord(ORGNR);
//        assertTrue(serviceRecord.isPresent());
//        ServiceRecord record = serviceRecord.get();
//        assertTrue(record instanceof DpeServiceRecord);
//        assertEquals(ORGNR, record.getOrganisationNumber());
//        assertTrue(!isNullOrEmpty(record.getPemCertificate()));
//    }
//
//    @Test
//    public void dpeShouldReturnEmptyRecord() {
//        when(lookupService.identifierHasInnsynskravCapability(any())).thenReturn(false);
//        assertTrue(!factory.createDpeInnsynServiceRecord(ORGNR).isPresent());
//    }
//
//    @Test
//    public void dpeShouldReturnErrorRecord() throws VirksertClientException {
//        when(lookupService.identifierHasInnsynskravCapability(any())).thenReturn(true);
//        when(virkSertService.getCertificate(any())).thenThrow(new CertificateNotFoundException("", mock(VirksertClientException.class)));
//
//        Optional<ServiceRecord> serviceRecord = factory.createDpeInnsynServiceRecord(ORGNR);
//        assertTrue(serviceRecord.isPresent());
//        ServiceRecord record = serviceRecord.get();
//        assertTrue(record instanceof ErrorServiceRecord);
//        assertEquals(DPE_INNSYN, record.getServiceIdentifier());
//    }
//
//    @Test
//    public void citizenShouldReturnDpiServiceRecord() throws KRRClientException {
//        PersonResource personResource = new PersonResource();
//        personResource.setCertificate("foo");
//        personResource.setAlertStatus("KAN_VARSLES");
//        personResource.setReserved("NEI");
//        personResource.setStatus("AKTIV");
//        DigitalPostResource dpr = DigitalPostResource.of("adr123", "adr321");
//        personResource.setDigitalPost(dpr);
//        when(krrService.getCitizenInfo(any())).thenReturn(personResource);
//
//        Authentication authMock = mock(Authentication.class);
//        OAuth2AuthenticationDetails detailsMock = mock(OAuth2AuthenticationDetails.class);
//        when(detailsMock.getTokenValue()).thenReturn("token");
//        when(authMock.getDetails()).thenReturn(detailsMock);
//
//        Optional<ServiceRecord> record = factory.createServiceRecordForCitizen("123", authMock, "123", Notification.OBLIGATED, false);
//        assertTrue(record.isPresent());
//        assertTrue(record.get() instanceof SikkerDigitalPostServiceRecord);
//    }
//
//    @Test
//    public void citizenShouldReturnDpiPrintServiceRecord_UserReserved() throws KRRClientException, BrregNotFoundException {
//        PersonResource personResource = new PersonResource();
//        personResource.setCertificate("foo");
//        personResource.setAlertStatus("KAN_VARSLES");
//        personResource.setReserved("JA");
//        personResource.setStatus("AKTIV");
//        DigitalPostResource dpr = DigitalPostResource.of("adr123", "adr321");
//        personResource.setDigitalPost(dpr);
//        when(krrService.getCitizenInfo(any())).thenReturn(personResource);
//
//        DSFResource dsf = new DSFResource();
//        dsf.setPostAddress("0101");
//        dsf.setName("foobar");
//        dsf.setStreet("foo road");
//        dsf.setCountry("Norway");
//        when(krrService.getDSFInfo(any())).thenReturn(Optional.of(dsf));
//        BrregEnhet brregEnhet = new BrregEnhet();
//        brregEnhet.setNavn("foo");
//        brregEnhet.setOrganisasjonsform("ORGL");
//        brregEnhet.setOrganisasjonsnummer("123");
//        brregEnhet.setPostadresse(new BrregPostadresse("foo road 123", "0001", "Oslo", "Norway"));
//        OrganizationInfo orginfo = OrganizationInfo.of(brregEnhet);
//        when(brregService.getOrganizationInfo("123")).thenReturn(Optional.of(orginfo));
//
//        Authentication authMock = mock(Authentication.class);
//        OAuth2AuthenticationDetails detailsMock = mock(OAuth2AuthenticationDetails.class);
//        when(detailsMock.getTokenValue()).thenReturn("token");
//        when(authMock.getDetails()).thenReturn(detailsMock);
//
//        Optional<ServiceRecord> record = factory.createServiceRecordForCitizen("123", authMock, "123", Notification.OBLIGATED, false);
//        verify(krrService).setPrintDetails(any());
//        assertTrue(record.isPresent());
//        assertTrue(record.get() instanceof SikkerDigitalPostServiceRecord);
//    }
//
//    @Test
//    public void citizenShouldReturnDpvServiceRecord_UserNotRegistered() throws KRRClientException {
//        PersonResource personResource = new PersonResource();
//        personResource.setCertificate("foo");
//        personResource.setAlertStatus("KAN_VARSLES");
//        personResource.setReserved("NEI");
//        personResource.setStatus("IKKE_REGISTRERT");
//        DigitalPostResource dpr = DigitalPostResource.of("adr123", "adr321");
//        personResource.setDigitalPost(dpr);
//        when(krrService.getCitizenInfo(any())).thenReturn(personResource);
//
//        Authentication authMock = mock(Authentication.class);
//        OAuth2AuthenticationDetails detailsMock = mock(OAuth2AuthenticationDetails.class);
//        when(detailsMock.getTokenValue()).thenReturn("token");
//        when(authMock.getDetails()).thenReturn(detailsMock);
//
//        Optional<ServiceRecord> record = factory.createServiceRecordForCitizen("123", authMock, "123", Notification.OBLIGATED, false);
//        assertTrue(record.isPresent());
//        assertTrue(record.get() instanceof PostVirksomhetServiceRecord);
//    }
}

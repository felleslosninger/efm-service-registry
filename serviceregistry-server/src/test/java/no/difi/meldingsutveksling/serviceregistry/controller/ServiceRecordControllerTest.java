package no.difi.meldingsutveksling.serviceregistry.controller;

import com.jayway.jsonpath.JsonPath;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import no.difi.meldingsutveksling.serviceregistry.CertificateNotFoundException;
import no.difi.meldingsutveksling.serviceregistry.config.SRConfig;
import no.difi.meldingsutveksling.serviceregistry.config.ServiceregistryProperties;
import no.difi.meldingsutveksling.serviceregistry.exceptions.SecurityLevelNotFoundException;
import no.difi.meldingsutveksling.serviceregistry.krr.*;
import no.difi.meldingsutveksling.serviceregistry.model.Process;
import no.difi.meldingsutveksling.serviceregistry.model.*;
import no.difi.meldingsutveksling.serviceregistry.security.PayloadSigner;
import no.difi.meldingsutveksling.serviceregistry.service.AuthenticationService;
import no.difi.meldingsutveksling.serviceregistry.service.EntityService;
import no.difi.meldingsutveksling.serviceregistry.service.ProcessService;
import no.difi.meldingsutveksling.serviceregistry.servicerecord.*;
import no.difi.meldingsutveksling.serviceregistry.svarut.SvarUtService;
import no.difi.virksert.client.lang.VirksertClientException;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.ByteArrayInputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.interfaces.RSAPublicKey;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest(value = ServiceRecordController.class)
@TestPropertySource("classpath:controller-test.properties")
@Import({PayloadSigner.class, SRConfig.class})
public class ServiceRecordControllerTest {

    private static final ArkivmeldingServiceRecord DPO_SERVICE_RECORD = ArkivmeldingServiceRecord.of(ServiceIdentifier.DPO, "42", "http://endpoint.here", "pem123");
    private static final ArkivmeldingServiceRecord DPV_SERVICE_RECORD = ArkivmeldingServiceRecord.of(ServiceIdentifier.DPV, "43", "http://endpoint.here");
    private static final ArkivmeldingServiceRecord DPF_SERVICE_RECORD = ArkivmeldingServiceRecord.of(ServiceIdentifier.DPF, "42", "http://endpoint.here", "pem234");
    private static final DpeServiceRecord DPE_SERVICE_RECORD = DpeServiceRecord.of("pem567", "50", ServiceIdentifier.DPE, "http://queue.here");

    @Autowired
    private MockMvc mvc;

    @MockBean
    private ServiceRecordFactory serviceRecordFactory;

    @MockBean
    private EntityService entityService;

    @MockBean
    private SvarUtService svarUtService;

    @MockBean
    private ProcessService processService;

    @MockBean
    private AuthenticationService authenticationService;

    @Autowired
    private PayloadSigner payloadSigner;

    @Before
    public void setup() {
        BrregPostadresse testAdr = new BrregPostadresse("testadresse", "1337", "teststed", "testland");
        OrganizationInfo ORGLinfo = new OrganizationInfo("42", "foo",
                testAdr, new OrganizationType("ORGL"));
        OrganizationInfo ORGinfo = new OrganizationInfo("50", "bar",
                testAdr, new OrganizationType("ORGL"));

        when(entityService.getEntityInfo("42")).thenReturn(Optional.of(ORGLinfo));
        when(entityService.getEntityInfo("50")).thenReturn(Optional.of(ORGinfo));
        OrganizationInfo ASinfo = new OrganizationInfo("43", "foo",
                testAdr, new OrganizationType("AS"));
        when(entityService.getEntityInfo("43")).thenReturn(Optional.of(ASinfo));
        CitizenInfo citizenInfo = new CitizenInfo("12345678901");
        when(entityService.getEntityInfo("12345678901")).thenReturn(Optional.of(citizenInfo));
        when(entityService.getEntityInfo("1337")).thenReturn(Optional.empty());
        assignServiceCodes(DPO_SERVICE_RECORD.getService(), "123", "321");
        assignServiceCodes(DPF_SERVICE_RECORD.getService(), "234", "432");
        assignServiceCodes(DPE_SERVICE_RECORD.getService(), "567", "765");
    }

    private void assignServiceCodes(SRService service, String serviceCode, String serviceEditionCode) {
        service.setServiceCode(serviceCode);
        service.setServiceEditionCode(serviceEditionCode);
    }

    @Test
    public void get_ArkivMeldingResolvesToDpo_ServiceRecordShouldMatchExpectedValues() throws Exception {
        when(serviceRecordFactory.createArkivmeldingServiceRecords(anyString(), any())).thenReturn(Lists.newArrayList(DPO_SERVICE_RECORD));
        mvc.perform(get("/identifier/42").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serviceRecords[0].organisationNumber", is("42")))
                .andExpect(jsonPath("$.serviceRecords[0].service.identifier", is("DPO")))
                .andExpect(jsonPath("$.serviceRecords[0].pemCertificate", is("-----BEGIN CERTIFICATE-----\npem123\n-----END CERTIFICATE-----\n")))
                .andExpect(jsonPath("$.serviceRecords[0].service.serviceCode", is("123")))
                .andExpect(jsonPath("$.serviceRecords[0].service.serviceEditionCode", is("321")))
                .andExpect(jsonPath("$.serviceRecords[0].service.endpointUrl", is("http://endpoint.here")))
                .andExpect(jsonPath("$.infoRecord.identifier", is("42")))
                .andExpect(jsonPath("$.infoRecord.entityType.name", is("ORGL")));
    }

    @Test
    public void get_ArkivmeldingResultsInCertificateException_ServiceRecordShouldMatchExpectedValues() throws Exception {
        when(serviceRecordFactory.createArkivmeldingServiceRecords(anyString(), any())).thenReturn(Lists.newArrayList(DPO_SERVICE_RECORD));
        final String message = "Certificate not found.";
        when(serviceRecordFactory.createArkivmeldingServiceRecords(anyString(), anyInt()))
                .thenThrow(new CertificateNotFoundException(message, new VirksertClientException("")));

        mvc.perform(get("/identifier/42").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_description", is(message)));
    }

    @Test
    public void get_ArkivmeldingResolvesToDpv_ServiceRecordShouldMatchExpectedValues() throws Exception {
        when(serviceRecordFactory.createArkivmeldingServiceRecords(anyString(), any())).thenReturn(Lists.newArrayList(DPV_SERVICE_RECORD));
        mvc.perform(get("/identifier/43").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serviceRecords[0].organisationNumber", is("43")))
                .andExpect(jsonPath("$.serviceRecords[0].service.identifier", is("DPV")))
                .andExpect(jsonPath("$.serviceRecords[0].pemCertificate", isEmptyOrNullString()))
                .andExpect(jsonPath("$.serviceRecords[0].service.endpointUrl", is("http://endpoint.here")))
                .andExpect(jsonPath("$.infoRecord.identifier", is("43")))
                .andExpect(jsonPath("$.infoRecord.organizationName", is("foo")))
                .andExpect(jsonPath("$.infoRecord.entityType.name", is("AS")));
    }

    @Test
    public void get_CredentialsResolveToDpi_ServiceRecordShouldMatchExpectedValues() throws Exception {
        setupMocksForSuccessfulDpi();
        mvc.perform(get("/identifier/12345678901").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serviceRecords[0].organisationNumber", is("12345678901")))
                .andExpect(jsonPath("$.serviceRecords[0].service.identifier", is("DPI")));
    }

    @Test
    public void get_CredentialsResolveToDpiAndDsfLookupFails_ShouldReturnErrorResponseBody() throws Exception {
        setupMocksForSuccessfulDpi();
        final String message = "identifier not found in DSF";
        when(serviceRecordFactory.createDigitalpostServiceRecords(anyString(), any(), anyString(), any(), anyBoolean()))
                .thenThrow(new DsfLookupException(message));

        mvc.perform(get("/identifier/12345678901")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_description", containsString(message)));
    }

    @Test
    public void getWithProcessIdentifier_CredentialsResolveToDpi_ServiceRecordShouldMatchExpectedValues() throws Exception {
        Process processMock = mockProcess(ProcessCategory.DIGITALPOST);
        when(processService.findByIdentifier(anyString())).thenReturn(Optional.of(processMock));
        setupMocksForSuccessfulDpi();

        mvc.perform(get("/identifier/12345678901/process/ProcessId").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serviceRecords[0].organisationNumber", is("12345678901")))
                .andExpect(jsonPath("$.serviceRecords[0].service.identifier", is("DPI")));
    }

    private void setupMocksForSuccessfulDpi() throws MalformedURLException, KRRClientException, DsfLookupException, SecurityLevelNotFoundException, CertificateNotFoundException {
        ServiceregistryProperties serviceregistryProperties = fakePropertiesForDpi();
        when(authenticationService.getAuthorizedClientIdentifier(any(), any())).thenReturn("AuthorizedIdentifier");
        PersonResource personResource = fakePersonResourceForDpi();
        PostAddress postAddress = new PostAddress("Address name", "Street x", "Postal code", "Area", "Country");
        SikkerDigitalPostServiceRecord dpiServiceRecord
                = new SikkerDigitalPostServiceRecord(serviceregistryProperties, personResource, ServiceIdentifier.DPI, "12345678901", postAddress, postAddress);
        when(serviceRecordFactory.createDigitalpostServiceRecords(anyString(), any(), anyString(), any(), anyBoolean()))
                .thenReturn(Lists.newArrayList(dpiServiceRecord));
        when(serviceRecordFactory.createArkivmeldingServiceRecord(anyString(), anyString(), anyInt())).thenReturn(Optional.empty());
        when(serviceRecordFactory.createEinnsynServiceRecords(anyString())).thenReturn(Lists.newArrayList());
    }

    @Test
    public void getWithProcessIdentifier_CredentialsResolveToDpiAndDsfLookupFails_ShouldReturnErrorResponseBody() throws Exception {
        Process processMock = mockProcess(ProcessCategory.DIGITALPOST);
        when(processService.findByIdentifier(anyString())).thenReturn(Optional.of(processMock));
        setupMocksForSuccessfulDpi();
        final String message = "identifier not found in DSF";
        when(serviceRecordFactory.createDigitalpostServiceRecords(anyString(), any(), anyString(), any(), anyBoolean()))
                .thenThrow(new DsfLookupException(message));

        mvc.perform(get("/identifier/12345678901")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_description", containsString(message)));
    }

    @Test
    public void get_CredentialsResolveToDpiAndLookupGivesError_ShouldReturnErrorResponseBody() throws Exception {
        final String message = "Error looking up identifier in KRR";
        when(authenticationService.getAuthorizedClientIdentifier(any(), any())).thenReturn("AuthorizedIdentifier");
        when(serviceRecordFactory.createDigitalpostServiceRecords(anyString(), any(), anyString(), any(), anyBoolean()))
                .thenThrow(new KRRClientException(new Exception(message)));

        mvc.perform(get("/identifier/12345678901")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error_description", containsString(message)));
    }

    @Test
    public void getWithProcessIdentifier_CredentialsResolveToDpiAndLookupGivesError_ShouldReturnErrorResponseBody() throws Exception {
        Process processMock = mockProcess(ProcessCategory.DIGITALPOST);
        when(processService.findByIdentifier(anyString())).thenReturn(Optional.of(processMock));
        final String message = "Error looking up identifier in KRR";
        when(authenticationService.getAuthorizedClientIdentifier(any(), any())).thenReturn("AuthorizedIdentifier");
        when(serviceRecordFactory.createDigitalpostServiceRecords(anyString(), any(), anyString(), any(), anyBoolean()))
                .thenThrow(new KRRClientException(new Exception(message)));

        mvc.perform(get("/identifier/12345678901/process/ProcessID")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error_description", containsString(message)));
    }

    private PersonResource fakePersonResourceForDpi() {
        PersonResource personResource = new PersonResource();
        personResource.setCertificate("cert123");
        personResource.setDigitalPost(DigitalPostResource.of("adr123", "post123"));
        personResource.setAlertStatus("KAN_VARSLES");
        personResource.setContactInfo(ContactInfoResource.of("post@post.foo", "", "123", ""));
        personResource.setReserved("NEI");
        personResource.setPrintPostkasseLeverandorAdr("postkasse123");
        return personResource;
    }

    private ServiceregistryProperties fakePropertiesForDpi() throws MalformedURLException {
        ServiceregistryProperties serviceregistryProperties = new ServiceregistryProperties();
        ServiceregistryProperties.DigitalPostInnbygger dpiConfig = new ServiceregistryProperties.DigitalPostInnbygger();
        dpiConfig.setEndpointURL(new URL("http://dpi.endpoint.here"));
        serviceregistryProperties.setDpi(dpiConfig);
        return serviceregistryProperties;
    }

    @Test
    public void get_EntityNotFound_ShouldReturnNotFound() throws Exception {
        mvc.perform(get("/identifier/1337").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    public void getSigned_ArkivmeldingResolvesToDpo_ServiceRecordShouldMatchExpectedValues() throws Exception {
        when(serviceRecordFactory.createArkivmeldingServiceRecords(anyString(), any())).thenReturn(Lists.newArrayList(DPO_SERVICE_RECORD));
        MvcResult response = mvc.perform(get("/identifier/42").accept("application/jose"))
                .andExpect(status().isOk())
                .andReturn();

        String serializedJose = response.getResponse().getContentAsString();
        JWSObject jwsObject = JWSObject.parse(serializedJose);
        byte[] decode = jwsObject.getHeader().getX509CertChain().get(0).decode();
        Certificate certificate = CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(decode));
        JWSVerifier jwsVerifier = new RSASSAVerifier((RSAPublicKey) certificate.getPublicKey());

        assertTrue(jwsObject.verify(jwsVerifier));

        String payload = jwsObject.getPayload().toString();
        assertEquals("42", JsonPath.read(payload, "$.serviceRecords[0].organisationNumber"));
    }

    @Test
    public void getWithProcessIdentifier_MissingEntity_ShouldReturn404() throws Exception {
        mvc.perform(get("/identifier/1337/process/ProcessIdHere")).andExpect(status().isNotFound());
    }

    @Test
    public void getWithProcessIdentifier_MissingProcess_ShouldReturn404() throws Exception {
        when(processService.findByIdentifier(anyString())).thenReturn(Optional.empty());
        mvc.perform(get("/identifier/42/process/NotFound")).andExpect(status().isNotFound());
    }

    @Test
    public void getWithProcessIdentifier_ArkivmeldingResolvesToDpv_ServiceRecordShouldMatchExpectedValues() throws Exception {
        Process processMock = mockProcess(ProcessCategory.ARKIVMELDING);
        when(processService.findByIdentifier(anyString())).thenReturn(Optional.of(processMock));
        when(serviceRecordFactory.createArkivmeldingServiceRecord(anyString(), any(), anyInt())).thenReturn(Optional.of(DPV_SERVICE_RECORD));

        mvc.perform(get("/identifier/43/process/ProcessID").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serviceRecords[0].organisationNumber", is("43")))
                .andExpect(jsonPath("$.serviceRecords[0].service.identifier", is("DPV")))
                .andExpect(jsonPath("$.serviceRecords[0].pemCertificate", isEmptyOrNullString()))
                .andExpect(jsonPath("$.serviceRecords[0].service.endpointUrl", is("http://endpoint.here")))
                .andExpect(jsonPath("$.infoRecord.identifier", is("43")))
                .andExpect(jsonPath("$.infoRecord.organizationName", is("foo")))
                .andExpect(jsonPath("$.infoRecord.entityType.name", is("AS")));
    }

    @Test
    public void getWithProcessIdentifier_ArkivmeldingResolvesToDpo_ServiceRecordShouldMatchExpectedValues() throws Exception {
        Process processMock = mockProcess(ProcessCategory.ARKIVMELDING);
        when(processService.findByIdentifier(anyString())).thenReturn(Optional.of(processMock));
        when(serviceRecordFactory.createArkivmeldingServiceRecord(anyString(), anyString(), anyInt())).thenReturn(Optional.of(DPO_SERVICE_RECORD));

        mvc.perform(get("/identifier/42/process/ProcessID").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serviceRecords[0].organisationNumber", is("42")))
                .andExpect(jsonPath("$.serviceRecords[0].pemCertificate", is("-----BEGIN CERTIFICATE-----\npem123\n-----END CERTIFICATE-----\n")))
                .andExpect(jsonPath("$.serviceRecords[0].service.identifier", is("DPO")))
                .andExpect(jsonPath("$.serviceRecords[0].service.serviceCode", is("123")))
                .andExpect(jsonPath("$.serviceRecords[0].service.serviceEditionCode", is("321")))
                .andExpect(jsonPath("$.serviceRecords[0].service.endpointUrl", is("http://endpoint.here")))
                .andExpect(jsonPath("$.infoRecord.identifier", is("42")))
                .andExpect(jsonPath("$.infoRecord.entityType.name", is("ORGL")));
    }

    @Test
    public void getWithProcessIdentifier_ArkivmeldingResultsInCertificateException_ShouldReturnErrorResponse() throws Exception {
        Process processMock = mockProcess(ProcessCategory.ARKIVMELDING);
        when(processService.findByIdentifier(anyString())).thenReturn(Optional.of(processMock));
        final String message = "Certificate not found.";
        when(serviceRecordFactory.createArkivmeldingServiceRecord(anyString(), anyString(), anyInt()))
                .thenThrow(new CertificateNotFoundException(message, new VirksertClientException("")));

        mvc.perform(get("/identifier/42/process/ProcessID").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_description", is(message)));
    }

    @Test
    public void getWithProcessIdentifier_ArkivmeldingResolvesToDpfAndRequestedSecurityLevelIsAvailable_ServiceRecordShouldMatchExpectedValues() throws Exception {
        DPF_SERVICE_RECORD.getService().setSecurityLevel(3);
        Process processMock = mockProcess(ProcessCategory.ARKIVMELDING);
        when(processService.findByIdentifier(anyString())).thenReturn(Optional.of(processMock));
        when(serviceRecordFactory.createArkivmeldingServiceRecord(anyString(), anyString(), any())).thenReturn(Optional.of(DPF_SERVICE_RECORD));
        mvc.perform(get("/identifier/42/process/ProcessID?securityLevel=3").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serviceRecords[0].organisationNumber", is("42")))
                .andExpect(jsonPath("$.serviceRecords[0].pemCertificate", is("-----BEGIN CERTIFICATE-----\npem234\n-----END CERTIFICATE-----\n")))
                .andExpect(jsonPath("$.serviceRecords[0].service.identifier", is("DPF")))
                .andExpect(jsonPath("$.serviceRecords[0].service.securityLevel", is(3)))
                .andExpect(jsonPath("$.serviceRecords[0].service.serviceCode", is("234")))
                .andExpect(jsonPath("$.serviceRecords[0].service.serviceEditionCode", is("432")))
                .andExpect(jsonPath("$.serviceRecords[0].service.endpointUrl", is("http://endpoint.here")))
                .andExpect(jsonPath("$.infoRecord.identifier", is("42")))
                .andExpect(jsonPath("$.infoRecord.entityType.name", is("ORGL")));
    }

    @Test
    public void getWithProcessIdentifier_ArkivmeldingResolvesToDpfButRequestedSecurityLevelIsNotAvailable_ShouldReturnErrorResponseBody() throws Exception {
        Process processMock = mockProcess(ProcessCategory.ARKIVMELDING);
        when(processService.findByIdentifier(anyString())).thenReturn(Optional.of(processMock));
        final String message = "security level not found";
        when(serviceRecordFactory.createArkivmeldingServiceRecord(anyString(), anyString(), anyInt())).thenThrow(new SecurityLevelNotFoundException(message));

        mvc.perform(get("/identifier/42/process/ProcessID?securityLevel=3")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_description", is(message)));
    }

    @Test
    public void getWithProcessIdentifier_ArkivmeldingResolvesToEmptyRecord_ShouldReturnErrorResponseBody() throws Exception {
        Process processMock = mockProcess(ProcessCategory.ARKIVMELDING);
        when(processService.findByIdentifier(anyString())).thenReturn(Optional.of(processMock));
        when(serviceRecordFactory.createArkivmeldingServiceRecord(anyString(), anyString(), anyInt())).thenReturn(Optional.empty());

        MockHttpServletResponse result = mvc.perform(get("/identifier/42/process/ProcessID?securityLevel=2")
                .accept(MediaType.APPLICATION_JSON))
                .andReturn().getResponse();

        assertEquals(HttpStatus.NOT_FOUND.value(), result.getStatus());
    }

    @Test
    public void get_ArkivmeldingResolvesToDpfAndRequestedSecurityLevelIsAvailable_ServiceRecordShouldMatchExpectedValues() throws Exception {
        DPF_SERVICE_RECORD.getService().setSecurityLevel(3);
        when(serviceRecordFactory.createArkivmeldingServiceRecords(anyString(), any())).thenReturn(Lists.newArrayList(DPF_SERVICE_RECORD));
        mvc.perform(get("/identifier/42?securityLevel=3").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serviceRecords[0].organisationNumber", is("42")))
                .andExpect(jsonPath("$.serviceRecords[0].pemCertificate", is("-----BEGIN CERTIFICATE-----\npem234\n-----END CERTIFICATE-----\n")))
                .andExpect(jsonPath("$.serviceRecords[0].service.identifier", is("DPF")))
                .andExpect(jsonPath("$.serviceRecords[0].service.securityLevel", is(3)))
                .andExpect(jsonPath("$.serviceRecords[0].service.serviceCode", is("234")))
                .andExpect(jsonPath("$.serviceRecords[0].service.serviceEditionCode", is("432")))
                .andExpect(jsonPath("$.serviceRecords[0].service.endpointUrl", is("http://endpoint.here")))
                .andExpect(jsonPath("$.infoRecord.identifier", is("42")))
                .andExpect(jsonPath("$.infoRecord.entityType.name", is("ORGL")));
    }

    @Test
    public void get_ArkivmeldingResolvesToDpfButRequestedSecurityLevelIsNotAvailable_ShouldReturnErrorResponseBody() throws Exception {
        final String message = "security level not found";
        when(serviceRecordFactory.createArkivmeldingServiceRecords(anyString(), anyInt())).thenThrow(new SecurityLevelNotFoundException(message));
        mvc.perform(get("/identifier/42?securityLevel=3")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_description", is(message)));
    }

    @Test
    public void getWithProcessIdentifier_EinnsynServiceRecordShouldMatchExpectedValues() throws Exception {
        Process processMock = mockProcess(ProcessCategory.EINNSYN);
        when(processService.findByIdentifier(anyString())).thenReturn(Optional.of(processMock));
        when(serviceRecordFactory.createEinnsynServiceRecord(anyString(), anyString())).thenReturn(Optional.of(DPE_SERVICE_RECORD));

        mvc.perform(get("/identifier/50/process/ProcessID").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serviceRecords[0].organisationNumber", is("50")))
                .andExpect(jsonPath("$.serviceRecords[0].pemCertificate", is("-----BEGIN CERTIFICATE-----\npem567\n-----END CERTIFICATE-----\n")))
                .andExpect(jsonPath("$.serviceRecords[0].service.identifier", is("DPE")))
                .andExpect(jsonPath("$.serviceRecords[0].service.serviceCode", is("567")))
                .andExpect(jsonPath("$.serviceRecords[0].service.serviceEditionCode", is("765")))
                .andExpect(jsonPath("$.serviceRecords[0].service.endpointUrl", is("http://queue.here")))
                .andExpect(jsonPath("$.infoRecord.identifier", is("50")))
                .andExpect(jsonPath("$.infoRecord.entityType.name", is("ORGL")));
    }

    private Process mockProcess(ProcessCategory category) {
        Process processMock = mock(Process.class);
        when(processMock.getCategory()).thenReturn(category);
        return processMock;
    }
}

package no.difi.meldingsutveksling.serviceregistry.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import no.difi.meldingsutveksling.Notification;
import no.difi.meldingsutveksling.serviceregistry.config.SRConfig;
import no.difi.meldingsutveksling.serviceregistry.config.ServiceregistryProperties;
import no.difi.meldingsutveksling.serviceregistry.exceptions.SecurityLevelNotFoundException;
import no.difi.meldingsutveksling.serviceregistry.krr.*;
import no.difi.meldingsutveksling.serviceregistry.model.Process;
import no.difi.meldingsutveksling.serviceregistry.model.*;
import no.difi.meldingsutveksling.serviceregistry.response.ErrorResponse;
import no.difi.meldingsutveksling.serviceregistry.security.PayloadSigner;
import no.difi.meldingsutveksling.serviceregistry.service.EntityService;
import no.difi.meldingsutveksling.serviceregistry.service.ProcessService;
import no.difi.meldingsutveksling.serviceregistry.servicerecord.*;
import no.difi.meldingsutveksling.serviceregistry.svarut.SvarUtService;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.interfaces.RSAPublicKey;
import java.util.Optional;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
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
    private static final DpeServiceRecord DPE_SERVICE_RECORD = DpeServiceRecord.of("pem567", "50", ServiceIdentifier.DPE,  "http://queue.here");

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
    @WithMockUser(username = "user1", password = "pwd", roles = "USER")
    public void get_IdentifierAndCredentialsResolveToDpi_ServiceRecordShouldMatchExpectedValues() throws Exception {
        ServiceregistryProperties serviceregistryProperties = fakePropertiesForDpi();
        PersonResource personResource = fakePersonResourceForDpi();
        PostAddress postAddress = new PostAddress("Address name", "Street x", "Postal code", "Area", "Country");
        SikkerDigitalPostServiceRecord dpiServiceRecord
                = new SikkerDigitalPostServiceRecord(serviceregistryProperties, personResource, ServiceIdentifier.DPI, "12345678901", postAddress, postAddress);
        when(serviceRecordFactory.createDigitalpostServiceRecords(anyString(), any(), anyString(), any(Notification.class), anyBoolean()))
                .thenReturn(Lists.newArrayList(dpiServiceRecord));
        when(serviceRecordFactory.createArkivmeldingServiceRecords(anyString(), any())).thenReturn(Lists.newArrayList());
        when(serviceRecordFactory.createEinnsynServiceRecords(anyString())).thenReturn(Lists.newArrayList());

        mvc.perform(get("/identifier/12345678901").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serviceRecords[0].organisationNumber", is("12345678901")))
                .andExpect(jsonPath("$.serviceRecords[0].service.identifier", is("DPI")));
    }

    @Test
    public void get_IdentifierAndCredentialsResolveToDpiAndLookupGivesError_ShouldReturnErrorResponseBody() throws Exception {
        final String message = "Error looking up identifier in KRR";
        when(serviceRecordFactory.createDigitalpostServiceRecords(anyString(), any(), anyString(), any(Notification.class), anyBoolean()))
                .thenThrow(new KRRClientException(new Exception(message)));

        MockHttpServletResponse result = mvc.perform(get("/identifier/12345678901")
                .accept(MediaType.APPLICATION_JSON))
                .andReturn().getResponse();

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), result.getStatus());
        assertTrue(deserializeErrorResponse(result).getErrorDescription().contains(message));
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
    public void getWithProcessIdentifier_ArkivmeldingResolvesToDpo_ServiceRecordShouldMatchExpectedValues() throws Exception {
        Process processMock = mock(Process.class);
        when(processMock.getCategory()).thenReturn(ProcessCategory.ARKIVMELDING);
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
    public void getWithSecurityLevel_ArkivmeldingResolvesToDpfAndSecurityLevelIsAvailable_ServiceRecordShouldMatchExpectedValues() throws Exception {
        when(serviceRecordFactory.createArkivmeldingServiceRecords(anyString(), any())).thenReturn(Lists.newArrayList(DPF_SERVICE_RECORD));
        mvc.perform(get("/identifier/42?securityLevel=3").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serviceRecords[0].organisationNumber", is("42")))
                .andExpect(jsonPath("$.serviceRecords[0].pemCertificate", is("-----BEGIN CERTIFICATE-----\npem234\n-----END CERTIFICATE-----\n")))
                .andExpect(jsonPath("$.serviceRecords[0].service.identifier", is("DPF")))
                .andExpect(jsonPath("$.serviceRecords[0].service.serviceCode", is("234")))
                .andExpect(jsonPath("$.serviceRecords[0].service.serviceEditionCode", is("432")))
                .andExpect(jsonPath("$.serviceRecords[0].service.endpointUrl", is("http://endpoint.here")))
                .andExpect(jsonPath("$.infoRecord.identifier", is("42")))
                .andExpect(jsonPath("$.infoRecord.entityType.name", is("ORGL")));
    }

    @Test
    public void getWithSecurityLevel_ArkivmeldingResolvesToDpfButSecurityLevelIsNotAvailable_ShouldReturnErrorResponseBody() throws Exception {
        final String message = "security level not found";
        when(serviceRecordFactory.createArkivmeldingServiceRecords(anyString(), anyInt())).thenThrow(new SecurityLevelNotFoundException(message));

        MockHttpServletResponse result = mvc.perform(get("/identifier/42?securityLevel=3")
                .accept(MediaType.APPLICATION_JSON))
                .andReturn().getResponse();

        assertEquals(HttpStatus.BAD_REQUEST.value(), result.getStatus());
        assertEquals(message, deserializeErrorResponse(result).getErrorDescription());
    }

    private ErrorResponse deserializeErrorResponse(MockHttpServletResponse result) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(result.getContentAsString(), ErrorResponse.class);
    }

    @Test
    public void getWithProcessIdentifier_EinnsynServiceRecordShouldMatchExpectedValues() throws Exception {
        Process processMock = mock(Process.class);
        when(processMock.getCategory()).thenReturn(ProcessCategory.EINNSYN);
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
}

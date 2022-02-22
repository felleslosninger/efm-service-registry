package no.difi.meldingsutveksling.serviceregistry.controller;

import com.jayway.jsonpath.JsonPath;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import no.difi.meldingsutveksling.serviceregistry.CertificateNotFoundException;
import no.difi.meldingsutveksling.serviceregistry.SRRequestScope;
import no.difi.meldingsutveksling.serviceregistry.config.SRConfig;
import no.difi.meldingsutveksling.serviceregistry.config.ServiceregistryProperties;
import no.difi.meldingsutveksling.serviceregistry.domain.Process;
import no.difi.meldingsutveksling.serviceregistry.domain.*;
import no.difi.meldingsutveksling.serviceregistry.exceptions.SecurityLevelNotFoundException;
import no.difi.meldingsutveksling.serviceregistry.krr.*;
import no.difi.meldingsutveksling.serviceregistry.record.ServiceRecord;
import no.difi.meldingsutveksling.serviceregistry.record.ServiceRecordService;
import no.difi.meldingsutveksling.serviceregistry.record.SikkerDigitalPostServiceRecord;
import no.difi.meldingsutveksling.serviceregistry.security.PayloadSigner;
import no.difi.meldingsutveksling.serviceregistry.service.AuthenticationService;
import no.difi.meldingsutveksling.serviceregistry.service.EntityService;
import no.difi.meldingsutveksling.serviceregistry.service.ProcessService;
import no.difi.meldingsutveksling.serviceregistry.service.brreg.BrregNotFoundException;
import no.difi.meldingsutveksling.serviceregistry.svarut.SvarUtClientException;
import no.difi.meldingsutveksling.serviceregistry.svarut.SvarUtService;
import no.difi.virksert.client.lang.VirksertClientException;
import org.assertj.core.util.Lists;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.restdocs.headers.HeaderDescriptor;
import org.springframework.restdocs.headers.HeaderDocumentation;
import org.springframework.restdocs.request.ParameterDescriptor;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.ByteArrayInputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import java.util.Optional;

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(value = ServiceRecordController.class)
@TestPropertySource("classpath:application-test.properties")
@Import({PayloadSigner.class, SRConfig.class})
@WithMockUser
@AutoConfigureRestDocs
public class ServiceRecordControllerTest {

    private static ServiceRecord DPO_SERVICE_RECORD;
    private static ServiceRecord DPV_SERVICE_RECORD;
    private static ServiceRecord DPF_SERVICE_RECORD;
    private static ServiceRecord DPE_SERVICE_RECORD;

    private static final String PROC_ARKIVMELDING_ADMINISTRASJON = "urn:no:difi:profile:arkivmelding:administrasjon:ver1.0";
    private static final String PROC_ARKIVMELDING_TEKNISKE_TJENESTER = "urn:no:difi:profile:arkivmelding:tekniskeTjenester:ver1.0";
    private static final String PROC_EINNSYN_INNSYNSKRAV = "urn:no:difi:profile:einnsyn:innsynskrav:ver1.0";
    private static final String PROC_DIGITALPOST = "urn:no:difi:profile:digitalpost:info:ver1.0";

    private static final String DOC_ARKIVMELDING = "urn:no:difi:arkivmelding:xsd::arkivmelding";
    private static final String DOC_INNSYNSKRAV = "urn:no:difi:einnsyn:xsd::innsynskrav";
    private static final String DOC_DIGITAL = "urn:no:difi:digitalpost:xsd:digital::digital";
    private static final String DOC_PRINT = "urn:no:difi:digitalpost:xsd:fysisk::print";

    private static final String SC_DPO = "4192";
    private static final String SC_DPV = "4255";

    private static final String SEC_DPO = "270815";
    private static final String SEC_DPV = "9";

    @Autowired
    private MockMvc mvc;

    @MockBean
    private ServiceRecordService serviceRecordService;

    @MockBean
    private EntityService entityService;

    @MockBean
    private SvarUtService svarUtService;

    @MockBean
    private ProcessService processService;

    @MockBean
    private AuthenticationService authenticationService;

    @MockBean
    private SRRequestScope requestScope;

    @Autowired
    private PayloadSigner payloadSigner;

    @MockBean
    private JwtDecoder jwtDecoder;

    @BeforeEach
    public void setup() {
        Process adminProcess = new Process()
                .setIdentifier(PROC_ARKIVMELDING_ADMINISTRASJON)
                .setDocumentTypes(singletonList(new DocumentType().setIdentifier(DOC_ARKIVMELDING)));
        DPO_SERVICE_RECORD = new ServiceRecord(ServiceIdentifier.DPO, "123123123", adminProcess, "http://endpoint.here");
        DPO_SERVICE_RECORD.setPemCertificate("pem123");
        DPO_SERVICE_RECORD.getService().setServiceEditionCode(SEC_DPO);
        DPO_SERVICE_RECORD.getService().setServiceCode(SC_DPO);

        Process tekniskeTjenesterProcess = new Process()
                .setIdentifier(PROC_ARKIVMELDING_TEKNISKE_TJENESTER)
                .setDocumentTypes(singletonList(new DocumentType().setIdentifier(PROC_ARKIVMELDING_TEKNISKE_TJENESTER)));
        DPV_SERVICE_RECORD = new ServiceRecord(ServiceIdentifier.DPV, "123123123", tekniskeTjenesterProcess, "http://endpoint.here");
        DPV_SERVICE_RECORD.getService().setServiceCode(SC_DPV);
        DPV_SERVICE_RECORD.getService().setServiceEditionCode(SEC_DPV);

        DPF_SERVICE_RECORD = new ServiceRecord(ServiceIdentifier.DPF, "321321321", adminProcess, "http://endpoint.here");
        DPF_SERVICE_RECORD.setPemCertificate("pem234");

        Process innsynskravProcess = new Process()
                .setIdentifier(PROC_EINNSYN_INNSYNSKRAV)
                .setDocumentTypes(singletonList(new DocumentType().setIdentifier(DOC_INNSYNSKRAV)));
        DPE_SERVICE_RECORD = new ServiceRecord(ServiceIdentifier.DPE, "123123123", innsynskravProcess, "innsyn");
        DPE_SERVICE_RECORD.setPemCertificate("pem567");

        Postadresse testAdr = new Postadresse("Skrivarvegen 42", "1337", "teststed", "testland");
        OrganizationInfo info123123123 = new OrganizationInfo("123123123", "foo",
                testAdr, new OrganizationType("ORGL"));
        when(entityService.getEntityInfo("123123123")).thenReturn(Optional.of(info123123123));
        OrganizationInfo info321321312 = new OrganizationInfo("321321321", "bar",
                testAdr, new OrganizationType("ORGL"));
        when(entityService.getEntityInfo("321321321")).thenReturn(Optional.of(info321321312));

        CitizenInfo citizenInfo = new CitizenInfo("12345678901");
        when(entityService.getEntityInfo("12345678901")).thenReturn(Optional.of(citizenInfo));
        when(entityService.getEntityInfo("404040404")).thenReturn(Optional.empty());
    }

    private void setupMocksForSuccessfulDpi() throws MalformedURLException, KontaktInfoException, SecurityLevelNotFoundException, CertificateNotFoundException, BrregNotFoundException, SvarUtClientException {
        ServiceregistryProperties props = fakePropertiesForDpi();
        when(authenticationService.getAuthorizedClientIdentifier(any(), any())).thenReturn("AuthorizedIdentifier");
        PersonResource personResource = fakePersonResourceForDpi();
        PostAddress postAddress = new PostAddress("Address name", "Street x", "Postal code", "Area", "Country");
        Process digitalpostProcess = new Process().setIdentifier(PROC_DIGITALPOST)
                .setDocumentTypes(Arrays.asList(new DocumentType().setIdentifier(DOC_DIGITAL), new DocumentType().setIdentifier(DOC_PRINT)));
        SikkerDigitalPostServiceRecord dpiServiceRecord = new SikkerDigitalPostServiceRecord("12345678901", digitalpostProcess, personResource,
                props.getDpi().getEndpointURL().toString(), false, postAddress, postAddress);
        dpiServiceRecord.setProcess(PROC_DIGITALPOST);
        dpiServiceRecord.setDocumentTypes(Arrays.asList(DOC_DIGITAL, DOC_PRINT));
        when(serviceRecordService.createDigitalpostServiceRecords(anyString(), anyString(), anyBoolean()))
                .thenReturn(Lists.newArrayList(dpiServiceRecord));
        when(serviceRecordService.createDigitalpostServiceRecords(anyString(), anyString(), anyBoolean(), any(Process.class)))
            .thenReturn(Lists.newArrayList(dpiServiceRecord));
        when(serviceRecordService.createArkivmeldingServiceRecord(any(), any(), anyInt())).thenReturn(Optional.empty());
        when(serviceRecordService.createEinnsynServiceRecords(any(), any())).thenReturn(Lists.newArrayList());
    }

    private ParameterDescriptor getIdentifierParam() {
        return parameterWithName("identifier").description("Organization number, or personal identification number");
    }

    private ParameterDescriptor getProcessParam() {
        return parameterWithName("processIdentifier").description("Process identifier");
    }

    private ParameterDescriptor getSecurityLevelParam() {
        return parameterWithName("securityLevel").optional().description("Security level. Only applies to receivers on the KS Fiks platform. Default is highest available for receiver.");
    }

    private ParameterDescriptor getConversationIdParam() {
        return parameterWithName("conversationId").optional().description("Conversation ID for the request. Used for logging purposes.");
    }

    private HeaderDescriptor getAuthHeader() {
        return HeaderDocumentation.headerWithName("Authorization").description("Bearer <JWT>").optional();
    }

    @Test
    public void testOrgInfoRecordShouldMatchExpectedValues() throws Exception {
        mvc.perform(get("/info/{identifier}", "123123123")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.infoRecord.identifier", is("123123123")))
                .andExpect(jsonPath("$.infoRecord.organizationName", is("foo")))
                .andDo(document("info/org",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(getAuthHeader()),
                        pathParameters(getIdentifierParam())
                ));
    }

    @Test
    public void testPersonInfoRecordShouldMatchExpectedValues() throws Exception {
        setupMocksForSuccessfulDpi();
        mvc.perform(get("/info/{identifier}", "12345678901")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.infoRecord.identifier", is("12345678901")))
                .andExpect(jsonPath("$.infoRecord.entityType.name", is("citizen")))
                .andDo(document("info/person",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(getAuthHeader()),
                        pathParameters(getIdentifierParam())
                ));
    }

    @Test
    public void get_ArkivMeldingResolvesToDpo_ServiceRecordShouldMatchExpectedValues() throws Exception {
        when(serviceRecordService.createArkivmeldingServiceRecords(any(), any()))
                .thenReturn(Lists.newArrayList(DPO_SERVICE_RECORD, DPV_SERVICE_RECORD, DPE_SERVICE_RECORD));
        mvc.perform(get("/identifier/{identifier}", "123123123")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serviceRecords[0].organisationNumber", is("123123123")))
                .andExpect(jsonPath("$.serviceRecords[0].service.identifier", is("DPO")))
                .andExpect(jsonPath("$.serviceRecords[0].pemCertificate", is("-----BEGIN CERTIFICATE-----\npem123\n-----END CERTIFICATE-----\n")))
                .andExpect(jsonPath("$.serviceRecords[0].service.serviceCode", is(SC_DPO)))
                .andExpect(jsonPath("$.serviceRecords[0].service.serviceEditionCode", is(SEC_DPO)))
                .andExpect(jsonPath("$.serviceRecords[0].service.endpointUrl", is("http://endpoint.here")))
                .andExpect(jsonPath("$.infoRecord.identifier", is("123123123")))
                .andExpect(jsonPath("$.infoRecord.entityType.name", is("ORGL")))
                .andDo(document("identifier/org",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(getAuthHeader()),
                        pathParameters(getIdentifierParam()),
                        requestParameters(getSecurityLevelParam(), getConversationIdParam())
                ));
    }

    @Test
    public void get_ArkivmeldingResultsInCertificateException_ServiceRecordShouldMatchExpectedValues() throws Exception {
        final String message = "Certificate not found.";
        when(serviceRecordService.createArkivmeldingServiceRecords(any(), any()))
                .thenThrow(new CertificateNotFoundException(message, new VirksertClientException("")));

        mvc.perform(get("/identifier/123123123").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error_description", is(message)));
    }

    @Test
    public void get_ArkivmeldingResultsInSvarUtClientException_ShouldReturnInternalServerError() throws Exception {
        final String message = "svarut is unavailable";
        when(serviceRecordService.createArkivmeldingServiceRecords(any(), any()))
                .thenThrow(new SvarUtClientException(new RuntimeException(message)));
        mvc.perform(get("/identifier/123123123").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error_description", Matchers.containsString(message)));
    }

    @Test
    public void get_ArkivmeldingResolvesToDpv_ServiceRecordShouldMatchExpectedValues() throws Exception {
        when(serviceRecordService.createArkivmeldingServiceRecords(any(), any())).thenReturn(Lists.newArrayList(DPV_SERVICE_RECORD));
        mvc.perform(get("/identifier/123123123").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serviceRecords[0].organisationNumber", is("123123123")))
                .andExpect(jsonPath("$.serviceRecords[0].service.identifier", is("DPV")))
                .andExpect(jsonPath("$.serviceRecords[0].pemCertificate", is(emptyOrNullString())))
                .andExpect(jsonPath("$.serviceRecords[0].service.endpointUrl", is("http://endpoint.here")))
                .andExpect(jsonPath("$.infoRecord.identifier", is("123123123")))
                .andExpect(jsonPath("$.infoRecord.organizationName", is("foo")))
                .andExpect(jsonPath("$.infoRecord.entityType.name", is("ORGL")));
    }

    @Test
    public void get_CredentialsResolveToDpi_ServiceRecordShouldMatchExpectedValues() throws Exception {
        setupMocksForSuccessfulDpi();
        mvc.perform(get("/identifier/{identifier}", "12345678901")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serviceRecords[0].organisationNumber", is("12345678901")))
                .andExpect(jsonPath("$.serviceRecords[0].service.identifier", is("DPI")))
                .andDo(document("identifier/person",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(getAuthHeader()),
                        pathParameters(getIdentifierParam()),
                        requestParameters(getSecurityLevelParam(), getConversationIdParam())
                ));
    }

    @Test
    public void get_CredentialsResolveToDpiAndDsfLookupFails_ShouldReturnErrorResponseBody() throws Exception {
        setupMocksForSuccessfulDpi();
        final String message = "identifier not found in DSF";
        when(serviceRecordService.createDigitalpostServiceRecords(anyString(), anyString(), anyBoolean()))
                .thenThrow(new KontaktInfoException(message));

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

        mvc.perform(get("/identifier/{identifier}/process/{processIdentifier}", "12345678901", PROC_DIGITALPOST)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serviceRecords[0].organisationNumber", is("12345678901")))
                .andExpect(jsonPath("$.serviceRecords[0].service.identifier", is("DPI")))
                .andDo(document("identifier/digital",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(getAuthHeader()),
                        pathParameters(getIdentifierParam(), getProcessParam()),
                        requestParameters(getSecurityLevelParam(), getConversationIdParam())
                ));
    }

    @Test
    public void getWithProcessIdentifier_CredentialsResolveToDpiAndDsfLookupFails_ShouldReturnErrorResponseBody() throws Exception {
        Process processMock = mockProcess(ProcessCategory.DIGITALPOST);
        when(processService.findByIdentifier(anyString())).thenReturn(Optional.of(processMock));
        setupMocksForSuccessfulDpi();
        final String message = "identifier not found in DSF";
        when(serviceRecordService.createDigitalpostServiceRecords(anyString(), anyString(), anyBoolean()))
                .thenThrow(new KontaktInfoException(message));

        mvc.perform(get("/identifier/12345678901")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_description", containsString(message)));
    }

    @Test
    public void get_CredentialsResolveToDpiAndLookupGivesError_ShouldReturnErrorResponseBody() throws Exception {
        final String message = "Error looking up identifier in KRR";
        when(authenticationService.getAuthorizedClientIdentifier(any(), any())).thenReturn("AuthorizedIdentifier");
        when(serviceRecordService.createDigitalpostServiceRecords(anyString(), anyString(), anyBoolean()))
                .thenThrow(new KontaktInfoException(message));

        mvc.perform(get("/identifier/12345678901")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_description", containsString(message)));
    }

    @Test
    public void getWithProcessIdentifier_CredentialsResolveToDpiAndLookupGivesError_ShouldReturnErrorResponseBody() throws Exception {
        Process processMock = mockProcess(ProcessCategory.DIGITALPOST);
        when(processService.findByIdentifier(anyString())).thenReturn(Optional.of(processMock));
        final String message = "Error looking up identifier in KRR";
        when(authenticationService.getAuthorizedClientIdentifier(any(), any())).thenReturn("AuthorizedIdentifier");
        when(serviceRecordService.createDigitalpostServiceRecords(anyString(), anyString(), anyBoolean(), any(Process.class)))
                .thenThrow(new KontaktInfoException(message));

        mvc.perform(get("/identifier/12345678901/process/some:process")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
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
        mvc.perform(get("/identifier/{identifier}", "404040404")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andDo(document("identifier/notfound",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(getIdentifierParam()),
                        requestParameters(getSecurityLevelParam(), getConversationIdParam())
                ));
    }

    @Test
    public void getSigned_ArkivmeldingResolvesToDpo_ServiceRecordShouldMatchExpectedValues() throws Exception {
        when(serviceRecordService.createArkivmeldingServiceRecords(any(), any())).thenReturn(Lists.newArrayList(DPO_SERVICE_RECORD));
        MvcResult response = mvc.perform(get("/identifier/123123123").accept("application/jose"))
                .andExpect(status().isOk())
                .andReturn();

        String serializedJose = response.getResponse().getContentAsString();
        JWSObject jwsObject = JWSObject.parse(serializedJose);
        byte[] decode = jwsObject.getHeader().getX509CertChain().get(0).decode();
        Certificate certificate = CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(decode));
        JWSVerifier jwsVerifier = new RSASSAVerifier((RSAPublicKey) certificate.getPublicKey());

        assertTrue(jwsObject.verify(jwsVerifier));

        String payload = jwsObject.getPayload().toString();
        assertEquals("123123123", JsonPath.read(payload, "$.serviceRecords[0].organisationNumber"));
    }

    @Test
    public void getWithProcessIdentifier_MissingEntity_ShouldReturn404() throws Exception {
        mvc.perform(get("/identifier/1337/process/ProcessIdHere")).andExpect(status().isNotFound());
    }

    @Test
    public void getWithProcessIdentifier_MissingProcess_ShouldReturn404() throws Exception {
        when(processService.findByIdentifier(anyString())).thenReturn(Optional.empty());
        mvc.perform(get("/identifier/123123123/process/NotFound")).andExpect(status().isNotFound());
    }

    @Test
    public void getWithProcessIdentifier_ArkivmeldingResolvesToDpv_ServiceRecordShouldMatchExpectedValues() throws Exception {
        Process processMock = mockProcess(ProcessCategory.ARKIVMELDING);
        when(processService.findByIdentifier(anyString())).thenReturn(Optional.of(processMock));
        when(serviceRecordService.createArkivmeldingServiceRecord(any(), any(), any())).thenReturn(Optional.of(DPV_SERVICE_RECORD));

        mvc.perform(get("/identifier/{identifier}/process/{processIdentifier}", "123123123", PROC_ARKIVMELDING_TEKNISKE_TJENESTER)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serviceRecords[0].organisationNumber", is("123123123")))
                .andExpect(jsonPath("$.serviceRecords[0].service.identifier", is("DPV")))
                .andExpect(jsonPath("$.serviceRecords[0].pemCertificate", is(emptyOrNullString())))
                .andExpect(jsonPath("$.serviceRecords[0].service.endpointUrl", is("http://endpoint.here")))
                .andExpect(jsonPath("$.infoRecord.identifier", is("123123123")))
                .andExpect(jsonPath("$.infoRecord.organizationName", is("foo")))
                .andExpect(jsonPath("$.infoRecord.entityType.name", is("ORGL")))
                .andDo(document("identifier/dpv",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(getAuthHeader()),
                        pathParameters(getIdentifierParam(), getProcessParam()),
                        requestParameters(getSecurityLevelParam(), getConversationIdParam())
                ));
    }

    @Test
    public void getWithProcessIdentifier_ArkivmeldingResolvesToDpo_ServiceRecordShouldMatchExpectedValues() throws Exception {
        Process processMock = mockProcess(ProcessCategory.ARKIVMELDING);
        when(processService.findByIdentifier(anyString())).thenReturn(Optional.of(processMock));
        when(serviceRecordService.createArkivmeldingServiceRecord(any(), any(), any())).thenReturn(Optional.of(DPO_SERVICE_RECORD));

        mvc.perform(get("/identifier/{identifier}/process/{processIdentifier}", "123123123", PROC_ARKIVMELDING_ADMINISTRASJON)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serviceRecords[0].organisationNumber", is("123123123")))
                .andExpect(jsonPath("$.serviceRecords[0].pemCertificate", is("-----BEGIN CERTIFICATE-----\npem123\n-----END CERTIFICATE-----\n")))
                .andExpect(jsonPath("$.serviceRecords[0].service.identifier", is("DPO")))
                .andExpect(jsonPath("$.serviceRecords[0].service.serviceCode", is(SC_DPO)))
                .andExpect(jsonPath("$.serviceRecords[0].service.serviceEditionCode", is(SEC_DPO)))
                .andExpect(jsonPath("$.serviceRecords[0].service.endpointUrl", is("http://endpoint.here")))
                .andExpect(jsonPath("$.infoRecord.identifier", is("123123123")))
                .andExpect(jsonPath("$.infoRecord.entityType.name", is("ORGL")))
                .andDo(document("identifier/arkivmelding",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(getAuthHeader()),
                        pathParameters(getIdentifierParam(), getProcessParam()),
                        requestParameters(getSecurityLevelParam(), getConversationIdParam())
                ));
    }

    @Test
    public void getWithProcessIdentifier_AvtaltResolvesToDpo_ServiceRecordShouldMatchExpectedValues() throws Exception {
        Process processMock = mockProcess(ProcessCategory.AVTALT);
        when(processService.findByIdentifier(anyString())).thenReturn(Optional.of(processMock));
        when(serviceRecordService.createServiceRecord(any(), any(), any())).thenReturn(Optional.of(DPO_SERVICE_RECORD));

        mvc.perform(get("/identifier/123123123/process/ProcessID").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serviceRecords[0].organisationNumber", is("123123123")))
                .andExpect(jsonPath("$.serviceRecords[0].pemCertificate", is("-----BEGIN CERTIFICATE-----\npem123\n-----END CERTIFICATE-----\n")))
                .andExpect(jsonPath("$.serviceRecords[0].service.identifier", is("DPO")))
                .andExpect(jsonPath("$.serviceRecords[0].service.serviceCode", is(SC_DPO)))
                .andExpect(jsonPath("$.serviceRecords[0].service.serviceEditionCode", is(SEC_DPO)))
                .andExpect(jsonPath("$.serviceRecords[0].service.endpointUrl", is("http://endpoint.here")))
                .andExpect(jsonPath("$.infoRecord.identifier", is("123123123")))
                .andExpect(jsonPath("$.infoRecord.entityType.name", is("ORGL")));
    }

    @Test
    public void getWithProcessIdentifier_ArkivmeldingResultsInCertificateException_ShouldReturnErrorResponse() throws Exception {
        Process processMock = mockProcess(ProcessCategory.ARKIVMELDING);
        when(processService.findByIdentifier(anyString())).thenReturn(Optional.of(processMock));
        final String message = "Certificate not found.";
        when(serviceRecordService.createArkivmeldingServiceRecord(any(), any(), any()))
                .thenThrow(new CertificateNotFoundException(message, new VirksertClientException("")));

        mvc.perform(get("/identifier/123123123/process/ProcessID").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error_description", is(message)));
    }

    @Test
    public void getWithProcessIdentifier_AvtaltmeldingResultsInCertificateException_ShouldReturnErrorResponse() throws Exception {
        Process processMock = mockProcess(ProcessCategory.AVTALT);
        when(processService.findByIdentifier(anyString())).thenReturn(Optional.of(processMock));
        final String message = "Certificate not found.";
        when(serviceRecordService.createServiceRecord(any(), any(), any()))
                .thenThrow(new CertificateNotFoundException(message, new VirksertClientException("")));

        mvc.perform(get("/identifier/123123123/process/ProcessID").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error_description", is(message)));
    }

    @Test
    public void getWithProcessIdentifier_ArkivmeldingResolvesToDpfAndRequestedSecurityLevelIsAvailable_ServiceRecordShouldMatchExpectedValues() throws Exception {
        DPF_SERVICE_RECORD.getService().setSecurityLevel(3);
        Process processMock = mockProcess(ProcessCategory.ARKIVMELDING);
        when(processService.findByIdentifier(anyString())).thenReturn(Optional.of(processMock));
        when(serviceRecordService.createArkivmeldingServiceRecord(any(), any(), any())).thenReturn(Optional.of(DPF_SERVICE_RECORD));
        mvc.perform(get("/identifier/321321321/process/ProcessID?securityLevel=3").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serviceRecords[0].organisationNumber", is("321321321")))
                .andExpect(jsonPath("$.serviceRecords[0].pemCertificate", is("-----BEGIN CERTIFICATE-----\npem234\n-----END CERTIFICATE-----\n")))
                .andExpect(jsonPath("$.serviceRecords[0].service.identifier", is("DPF")))
                .andExpect(jsonPath("$.serviceRecords[0].service.securityLevel", is(3)))
                .andExpect(jsonPath("$.serviceRecords[0].service.endpointUrl", is("http://endpoint.here")))
                .andExpect(jsonPath("$.infoRecord.identifier", is("321321321")))
                .andExpect(jsonPath("$.infoRecord.entityType.name", is("ORGL")));
    }

    @Test
    public void getWithProcessIdentifier_ArkivmeldingResolvesToDpfButRequestedSecurityLevelIsNotAvailable_ShouldReturnErrorResponseBody() throws Exception {
        Process processMock = mockProcess(ProcessCategory.ARKIVMELDING);
        when(processService.findByIdentifier(anyString())).thenReturn(Optional.of(processMock));
        final String message = "security level not found";
        when(serviceRecordService.createArkivmeldingServiceRecord(any(), any(), anyInt())).thenThrow(new SecurityLevelNotFoundException(message));

        mvc.perform(get("/identifier/321321321/process/"+PROC_ARKIVMELDING_ADMINISTRASJON+"?securityLevel=4")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_description", is(message)));
    }

    @Test
    public void getWithProcessIdentifier_ArkivmeldingResolvesToEmptyRecord_ShouldReturnErrorResponseBody() throws Exception {
        Process processMock = mockProcess(ProcessCategory.ARKIVMELDING);
        when(processService.findByIdentifier(anyString())).thenReturn(Optional.of(processMock));
        when(serviceRecordService.createArkivmeldingServiceRecord(any(), any(), anyInt())).thenReturn(Optional.empty());

        MockHttpServletResponse result = mvc.perform(get("/identifier/123123123/process/some:invalid:process?securityLevel=2")
                .accept(MediaType.APPLICATION_JSON))
                .andReturn().getResponse();

        assertEquals(HttpStatus.NOT_FOUND.value(), result.getStatus());
    }

    @Test
    public void get_ArkivmeldingResolvesToDpfAndRequestedSecurityLevelIsAvailable_ServiceRecordShouldMatchExpectedValues() throws Exception {
        DPF_SERVICE_RECORD.getService().setSecurityLevel(3);
        when(serviceRecordService.createArkivmeldingServiceRecords(any(), any())).thenReturn(Lists.newArrayList(DPF_SERVICE_RECORD));
        mvc.perform(get("/identifier/321321321?securityLevel=3").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serviceRecords[0].organisationNumber", is("321321321")))
                .andExpect(jsonPath("$.serviceRecords[0].pemCertificate", is("-----BEGIN CERTIFICATE-----\npem234\n-----END CERTIFICATE-----\n")))
                .andExpect(jsonPath("$.serviceRecords[0].service.identifier", is("DPF")))
                .andExpect(jsonPath("$.serviceRecords[0].service.securityLevel", is(3)))
                .andExpect(jsonPath("$.serviceRecords[0].service.endpointUrl", is("http://endpoint.here")))
                .andExpect(jsonPath("$.infoRecord.identifier", is("321321321")))
                .andExpect(jsonPath("$.infoRecord.entityType.name", is("ORGL")));
    }

    @Test
    public void get_ArkivmeldingResolvesToDpfButRequestedSecurityLevelIsNotAvailable_ShouldReturnErrorResponseBody() throws Exception {
        final String message = "security level not found";
        when(serviceRecordService.createArkivmeldingServiceRecords(any(), anyInt())).thenThrow(new SecurityLevelNotFoundException(message));
        mvc.perform(get("/identifier/{identifier}?securityLevel=4", "321321321")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_description", is(message)))
                .andDo(document("identifier/sec-level-not-found",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(getIdentifierParam()),
                        requestParameters(getSecurityLevelParam(), getConversationIdParam())
                ));
    }

    @Test
    public void getWithProcessIdentifier_EinnsynServiceRecordShouldMatchExpectedValues() throws Exception {
        Process processMock = mockProcess(ProcessCategory.EINNSYN);
        when(processService.findByIdentifier(anyString())).thenReturn(Optional.of(processMock));
        when(serviceRecordService.createServiceRecord(any(), any(), any())).thenReturn(Optional.of(DPE_SERVICE_RECORD));

        mvc.perform(get("/identifier/{identifier}/process/{processIdentifier}", "123123123", PROC_EINNSYN_INNSYNSKRAV)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serviceRecords[0].organisationNumber", is("123123123")))
                .andExpect(jsonPath("$.serviceRecords[0].pemCertificate", is("-----BEGIN CERTIFICATE-----\npem567\n-----END CERTIFICATE-----\n")))
                .andExpect(jsonPath("$.serviceRecords[0].service.identifier", is("DPE")))
                .andExpect(jsonPath("$.serviceRecords[0].service.endpointUrl", is("innsyn")))
                .andExpect(jsonPath("$.infoRecord.identifier", is("123123123")))
                .andExpect(jsonPath("$.infoRecord.entityType.name", is("ORGL")))
                .andDo(document("identifier/einnsyn",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(getAuthHeader()),
                        pathParameters(getIdentifierParam(), getProcessParam()),
                        requestParameters(getSecurityLevelParam(), getConversationIdParam())
                ));
    }

    private Process mockProcess(ProcessCategory category) {
        Process processMock = mock(Process.class);
        when(processMock.getCategory()).thenReturn(category);
        return processMock;
    }

}

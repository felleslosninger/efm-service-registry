package no.difi.meldingsutveksling.serviceregistry.controller;

import com.jayway.jsonpath.JsonPath;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import no.difi.meldingsutveksling.serviceregistry.config.SRConfig;
import no.difi.meldingsutveksling.serviceregistry.config.ServiceregistryProperties;
import no.difi.meldingsutveksling.serviceregistry.krr.*;
import no.difi.meldingsutveksling.serviceregistry.model.Process;
import no.difi.meldingsutveksling.serviceregistry.model.*;
import no.difi.meldingsutveksling.serviceregistry.security.PayloadSigner;
import no.difi.meldingsutveksling.serviceregistry.service.EntityService;
import no.difi.meldingsutveksling.serviceregistry.service.ProcessService;
import no.difi.meldingsutveksling.serviceregistry.servicerecord.ArkivmeldingServiceRecord;
import no.difi.meldingsutveksling.serviceregistry.servicerecord.SRService;
import no.difi.meldingsutveksling.serviceregistry.servicerecord.ServiceRecordFactory;
import no.difi.meldingsutveksling.serviceregistry.svarut.SvarUtService;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
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

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
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
    private static final ArkivmeldingServiceRecord DPO_SERVICE_RECORD = ArkivmeldingServiceRecord.of(ServiceIdentifier.DPO, "42", "http://endpoint.here", "pem123");
    private final ArkivmeldingServiceRecord DPV_SERVICE_RECORD = ArkivmeldingServiceRecord.of(ServiceIdentifier.DPV, "43", "http://endpoint.here");

    @Before
    public void setup() throws MalformedURLException, KRRClientException {
        BrregPostadresse testAdr = new BrregPostadresse("testadresse", "1337", "teststed", "testland");
        OrganizationInfo ORGLinfo = new OrganizationInfo("42", "foo",
                testAdr, new OrganizationType("ORGL"));
        when(entityService.getEntityInfo("42")).thenReturn(Optional.of(ORGLinfo));
        OrganizationInfo ASinfo = new OrganizationInfo("43", "foo",
                testAdr, new OrganizationType("AS"));
        when(entityService.getEntityInfo("43")).thenReturn(Optional.of(ASinfo));
        CitizenInfo citizenInfo = new CitizenInfo("12345678901");
        when(entityService.getEntityInfo("12345678901")).thenReturn(Optional.of(citizenInfo));
        when(entityService.getEntityInfo("1337")).thenReturn(Optional.empty());

        ServiceregistryProperties serviceregistryProperties = new ServiceregistryProperties();
        ServiceregistryProperties.PostVirksomhet postVirksomhet = new ServiceregistryProperties.PostVirksomhet();
        postVirksomhet.setEndpointURL(new URL("http://foo"));
        serviceregistryProperties.setDpv(postVirksomhet);

        PostAddress postAddress = mock(PostAddress.class);
        PersonResource personResource = new PersonResource();
        personResource.setCertificate("cert123");
        personResource.setDigitalPost(DigitalPostResource.of("adr123", "post123"));
        personResource.setAlertStatus("KAN_VARSLES");
        personResource.setContactInfo(ContactInfoResource.of("post@post.foo", "", "123", ""));
        personResource.setReserved("NEI");
        personResource.setPrintPostkasseLeverandorAdr("postkasse123");

        SRService service = DPO_SERVICE_RECORD.getService();
        service.setServiceCode("123");
        service.setServiceEditionCode("321");

//        SikkerDigitalPostServiceRecord dpiServiceRecord = new SikkerDigitalPostServiceRecord(null, personResource,
//                ServiceIdentifier.DPI, "12345678901", postAddress, postAddress);
    }

    @Test
    public void get_ArkivMeldingResolvesToDpo_ServiceRecordShouldMatchExpectedValues() throws Exception {
        when(serviceRecordFactory.createArkivmeldingServiceRecords(anyString())).thenReturn(Lists.newArrayList(DPO_SERVICE_RECORD));
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
        when(serviceRecordFactory.createArkivmeldingServiceRecords(anyString())).thenReturn(Lists.newArrayList(DPV_SERVICE_RECORD));
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
    @Ignore
    public void dpiEntityShouldReturnOK() throws Exception {
        OAuth2Authentication mock = mock(OAuth2Authentication.class);
        when(mock.getName()).thenReturn("foo");
        when(mock.getPrincipal()).thenReturn("foo");

        mvc.perform(get("/identifier/12345678901").accept(MediaType.APPLICATION_JSON).with
                (SecurityMockMvcRequestPostProcessors.authentication(mock)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serviceRecord.organisationNumber", is("12345678901")))
                .andExpect(jsonPath("$.serviceRecord.serviceIdentifier", is("DPI")));
    }

    @Test
    public void entityShouldReturnNotFound() throws Exception {
        mvc.perform(get("/identifier/1337").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    public void signedShouldReturnOK() throws Exception {
        when(serviceRecordFactory.createArkivmeldingServiceRecords(anyString())).thenReturn(Lists.newArrayList(DPO_SERVICE_RECORD));
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
    public void entityAndProcessLookup_MissingEntity_ShouldReturn404() throws Exception {
        mvc.perform(get("/entity/1337?process=n/a")).andExpect(status().isNotFound());
    }

    @Test
    public void entityAndProcessLookup_MissingProcess_ShouldReturn404() throws Exception {
        when(processService.findByIdentifier(anyString())).thenReturn(Optional.empty());
        mvc.perform(get("/entity/42?process=notFound")).andExpect(status().isNotFound());
    }

    @Test
    public void getWithProcessIdentifier_ArkivmeldingResolvesToDpo_ServiceRecordShouldMatchExpectedValues() throws Exception {
        Process processMock = mock(Process.class);
        when(processMock.getCategory()).thenReturn(ProcessCategory.ARKIVMELDING);
        when(processService.findByIdentifier(anyString())).thenReturn(Optional.of(processMock));
        when(serviceRecordFactory.createArkivmeldingServiceRecord(anyString(), anyString())).thenReturn(DPO_SERVICE_RECORD);

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
}

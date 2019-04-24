package no.difi.meldingsutveksling.serviceregistry.controller;

import com.jayway.jsonpath.JsonPath;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import no.difi.meldingsutveksling.serviceregistry.config.SRConfig;
import no.difi.meldingsutveksling.serviceregistry.config.ServiceregistryProperties;
import no.difi.meldingsutveksling.serviceregistry.krr.*;
import no.difi.meldingsutveksling.serviceregistry.model.*;
import no.difi.meldingsutveksling.serviceregistry.security.PayloadSigner;
import no.difi.meldingsutveksling.serviceregistry.service.EntityService;
import no.difi.meldingsutveksling.serviceregistry.service.ProcessService;
import no.difi.meldingsutveksling.serviceregistry.servicerecord.ServiceRecordFactory;
import no.difi.meldingsutveksling.serviceregistry.servicerecord.SikkerDigitalPostServiceRecord;
import no.difi.meldingsutveksling.serviceregistry.svarut.SvarUtService;
import org.junit.Assert;
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

        SikkerDigitalPostServiceRecord dpiServiceRecord = new SikkerDigitalPostServiceRecord(null, personResource,
                ServiceIdentifier.DPI, "12345678901", postAddress, postAddress);
    }

    @Test
    public void eduEntityShouldReturnOK() throws Exception {
        mvc.perform(get("/identifier/42").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serviceRecord.organisationNumber", is("42")))
                .andExpect(jsonPath("$.serviceRecord.serviceIdentifier", is("DPO")))
                .andExpect(jsonPath("$.serviceRecord.pemCertificate", is("-----BEGIN CERTIFICATE-----\npem123\n-----END CERTIFICATE-----\n")))
                .andExpect(jsonPath("$.serviceRecord.serviceCode", is("123")))
                .andExpect(jsonPath("$.serviceRecord.serviceEditionCode", is("321")))
                .andExpect(jsonPath("$.serviceRecord.endPointURL", is("http://foo")))
                .andExpect(jsonPath("$.infoRecord.identifier", is("42")))
                .andExpect(jsonPath("$.infoRecord.entityType.name", is("ORGL")));
    }

    @Test
    public void dpvEntityShouldReturnOK() throws Exception {
        mvc.perform(get("/identifier/43").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serviceRecord.organisationNumber", is("43")))
                .andExpect(jsonPath("$.serviceRecord.serviceIdentifier", is("DPV")))
                .andExpect(jsonPath("$.serviceRecord.pemCertificate", isEmptyOrNullString()))
                .andExpect(jsonPath("$.serviceRecord.endPointURL", is("http://foo")))
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
        MvcResult response = mvc.perform(get("/identifier/42").accept("application/jose"))
                .andExpect(status().isOk())
                .andReturn();

        String serializedJose = response.getResponse().getContentAsString();
        JWSObject jwsObject = JWSObject.parse(serializedJose);
        byte[] decode = jwsObject.getHeader().getX509CertChain().get(0).decode();
        Certificate certificate = CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(decode));
        JWSVerifier jwsVerifier = new RSASSAVerifier((RSAPublicKey) certificate.getPublicKey());

        Assert.assertTrue(jwsObject.verify(jwsVerifier));

        String payload = jwsObject.getPayload().toString();
        Assert.assertEquals("42", JsonPath.read(payload, "$.serviceRecord.organisationNumber"));
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

//    @Test
//    public void entityAndProcessLookup_DpoRecordCreated_ShouldReturnOk() throws Exception {
//        Process dpoProcess = mock(Process.class);
//        when(dpoProcess.getCategory()).thenReturn(ProcessCategory.ARKIVMELDING);
//        when(processService.findByIdentifier(anyString())).thenReturn(Optional.of(dpoProcess));
//        EDUServiceRecord eduServiceRecord = new EDUServiceRecord("pem123", "http://foo", "123", "321", "42");
//        when(serviceRecordFactory.createDpoServiceRecord(anyString(), any(Process.class))).thenReturn(Optional.of(eduServiceRecord));
//
//        mvc.perform(get("/entity/42?process=ProcessID").accept(MediaType.APPLICATION_JSON))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.serviceRecord.organisationNumber", is("42")))
//                .andExpect(jsonPath("$.serviceRecord.serviceIdentifier", is("DPO")))
//                .andExpect(jsonPath("$.serviceRecord.pemCertificate", is("-----BEGIN CERTIFICATE-----\npem123\n-----END CERTIFICATE-----\n")))
//                .andExpect(jsonPath("$.serviceRecord.serviceCode", is("123")))
//                .andExpect(jsonPath("$.serviceRecord.serviceEditionCode", is("321")))
//                .andExpect(jsonPath("$.serviceRecord.endPointURL", is("http://foo")))
//                .andExpect(jsonPath("$.infoRecord.identifier", is("42")))
//                .andExpect(jsonPath("$.infoRecord.entityType.name", is("ORGL")));
//    }
}

package no.difi.meldingsutveksling.serviceregistry.controller;

import com.jayway.jsonpath.JsonPath;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import no.difi.meldingsutveksling.ptp.KontaktInfo;
import no.difi.meldingsutveksling.ptp.PostAddress;
import no.difi.meldingsutveksling.serviceregistry.config.SRConfig;
import no.difi.meldingsutveksling.serviceregistry.config.ServiceregistryProperties;
import no.difi.meldingsutveksling.serviceregistry.model.CitizenInfo;
import no.difi.meldingsutveksling.serviceregistry.model.OrganizationInfo;
import no.difi.meldingsutveksling.serviceregistry.model.OrganizationType;
import no.difi.meldingsutveksling.serviceregistry.model.ServiceIdentifier;
import no.difi.meldingsutveksling.serviceregistry.security.EntitySigner;
import no.difi.meldingsutveksling.serviceregistry.security.EntitySignerException;
import no.difi.meldingsutveksling.serviceregistry.service.EntityService;
import no.difi.meldingsutveksling.serviceregistry.service.ks.FiksAdresseClient;
import no.difi.meldingsutveksling.serviceregistry.service.ks.FiksAdressing;
import no.difi.meldingsutveksling.serviceregistry.servicerecord.EDUServiceRecord;
import no.difi.meldingsutveksling.serviceregistry.servicerecord.PostVirksomhetServiceRecord;
import no.difi.meldingsutveksling.serviceregistry.servicerecord.ServiceRecordFactory;
import no.difi.meldingsutveksling.serviceregistry.servicerecord.SikkerDigitalPostServiceRecord;
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

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest(value = ServiceRecordController.class)
@TestPropertySource("classpath:controller-test.properties")
@Import({EntitySigner.class, SRConfig.class})
public class ServiceRecordControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private ServiceRecordFactory serviceRecordFactory;

    @MockBean
    private EntityService entityService;

    @MockBean
    private FiksAdresseClient fiksAdresseClient;

    @Autowired
    private EntitySigner entitySigner;

    @Before
    public void setup() throws EntitySignerException, MalformedURLException {
        when(fiksAdresseClient.getFiksAdressing(anyString())).thenReturn(FiksAdressing.EMPTY);

        OrganizationInfo ORGLinfo = new OrganizationInfo("42", "foo", OrganizationType.from("ORGL"));
        when(entityService.getEntityInfo("42")).thenReturn(ORGLinfo);
        OrganizationInfo ASinfo = new OrganizationInfo("43", "foo", OrganizationType.from("AS"));
        when(entityService.getEntityInfo("43")).thenReturn(ASinfo);
        CitizenInfo citizenInfo = new CitizenInfo("12345678901");
        when(entityService.getEntityInfo("12345678901")).thenReturn(citizenInfo);

        EDUServiceRecord dpoServiceRecord = new EDUServiceRecord("pem123", "http://foo", "123", "321", "42");
        when(serviceRecordFactory.createEduServiceRecord("42")).thenReturn(dpoServiceRecord);

        ServiceregistryProperties serviceregistryProperties = new ServiceregistryProperties();
        ServiceregistryProperties.PostVirksomhet postVirksomhet = new ServiceregistryProperties.PostVirksomhet();
        postVirksomhet.setEndpointURL(new URL("http://foo"));
        serviceregistryProperties.setDpv(postVirksomhet);
        PostVirksomhetServiceRecord dpvServiceRecord = new PostVirksomhetServiceRecord(serviceregistryProperties, "43");
        when(serviceRecordFactory.createPostVirksomhetServiceRecord("43")).thenReturn(dpvServiceRecord);

        KontaktInfo kontaktInfoMock = mock(KontaktInfo.class);
        when(kontaktInfoMock.getCertificate()).thenReturn("cert123");
        when(kontaktInfoMock.getOrgnrPostkasse()).thenReturn("post123");
        when(kontaktInfoMock.getPostkasseAdresse()).thenReturn("adr123");
        when(kontaktInfoMock.isNotifiable()).thenReturn(false);
        when(kontaktInfoMock.getEpostadresse()).thenReturn("post@post.foo");
        when(kontaktInfoMock.getMobiltelefonnummer()).thenReturn("123456789");
        when(kontaktInfoMock.isReservert()).thenReturn(false);
        PostAddress postAddress = mock(PostAddress.class);
        PostAddress returnAddress = mock(PostAddress.class);
        SikkerDigitalPostServiceRecord dpiServiceRecord = new SikkerDigitalPostServiceRecord(null, kontaktInfoMock, ServiceIdentifier.DPI, "12345678901", postAddress, returnAddress);
        when(serviceRecordFactory.createServiceRecordForCititzen(eq("12345678901"), any(), any())).thenReturn
                (dpiServiceRecord);
    }

    @Test
    public void eduEntityShouldReturnOK() throws Exception {
        mvc.perform(get("/identifier/42").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serviceRecord.organisationNumber", is("42")))
                .andExpect(jsonPath("$.serviceRecord.serviceIdentifier", is("DPO")))
                .andExpect(jsonPath("$.serviceRecord.pemCertificate", is("pem123")))
                .andExpect(jsonPath("$.serviceRecord.serviceCode", is("123")))
                .andExpect(jsonPath("$.serviceRecord.serviceEditionCode", is("321")))
                .andExpect(jsonPath("$.serviceRecord.endPointURL", is("http://foo")))
                .andExpect(jsonPath("$.infoRecord.identifier", is("42")))
                .andExpect(jsonPath("$.infoRecord.entityType.name", is("Organisasjonsledd")))
                .andExpect(jsonPath("$.infoRecord.entityType.acronym", is("ORGL")));
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
                .andExpect(jsonPath("$.infoRecord.entityType.name", is("Aksjeselskap")))
                .andExpect(jsonPath("$.infoRecord.entityType.acronym", is("AS")));
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
}

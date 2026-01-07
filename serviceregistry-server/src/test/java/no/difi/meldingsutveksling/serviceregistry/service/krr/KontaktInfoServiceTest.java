package no.difi.meldingsutveksling.serviceregistry.service.krr;

import com.nimbusds.jose.jwk.RSAKey;
import network.oxalis.vefa.peppol.lookup.LookupClient;
import no.difi.meldingsutveksling.serviceregistry.MoveServiceRegistryApplication;
import no.difi.meldingsutveksling.serviceregistry.config.VirksertConfig;
import no.difi.meldingsutveksling.serviceregistry.freg.client.DefaultFregGatewayClient;
import no.difi.meldingsutveksling.serviceregistry.krr.KRRClient;
import no.difi.meldingsutveksling.serviceregistry.krr.KontaktInfoException;
import no.difi.meldingsutveksling.serviceregistry.krr.PersonResource;
import no.difi.meldingsutveksling.serviceregistry.record.LookupParameters;
import no.difi.meldingsutveksling.serviceregistry.service.brreg.BrregService;
import no.difi.meldingsutveksling.serviceregistry.service.elma.ELMALookupService;
import no.difi.meldingsutveksling.serviceregistry.service.elma.EformidlingLookupClientWrapper;
import no.difi.meldingsutveksling.serviceregistry.service.healthcare.NhnService;
import no.difi.meldingsutveksling.serviceregistry.service.virksert.VirkSertService;
import no.difi.meldingsutveksling.serviceregistry.svarut.SvarUtClient;
import no.difi.meldingsutveksling.serviceregistry.svarut.SvarUtService;
import no.difi.meldingsutveksling.serviceregistry.svarut.TokenService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.transport.http.AbstractHttpWebServiceMessageSender;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = MoveServiceRegistryApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class KontaktInfoServiceTest {
    @Autowired
    private KontaktInfoService kontaktInfoService;

    @MockitoBean
    private DefaultFregGatewayClient defaultFregGatewayClient;

    @MockitoBean
    private KRRClient krrClient;

    @MockitoBean
    private RSAKey rsaKey;

    @MockitoBean
    private VirkSertService virkSertService;

    @MockitoBean
    private VirksertConfig virksertConfig;

    @MockitoBean
    private BrregService brregService;

    @MockitoBean
    private NhnService nhnService;

    @MockitoBean
    private ELMALookupService elmaLookupService;

    @MockitoBean
    private LookupClient lookupClient;

    @MockitoBean
    private EformidlingLookupClientWrapper eformidlingLookupClientWrapper;

    @MockitoBean
    private SvarUtService svarUtService;

    @MockitoBean
    private SvarUtClient svarUtClient;

    @MockitoBean
    private WebServiceTemplate webServiceTemplate;

    @MockitoBean
    private AbstractHttpWebServiceMessageSender sender;

    @MockitoBean
    private TokenService tokenService;

    @Test
    public void testRetry() throws KontaktInfoException, MalformedURLException, URISyntaxException {
        PersonResource personResource = new PersonResource();
        LookupParameters lookupParameters = mock(LookupParameters.class);
        Jwt jwt = mock(Jwt.class);
        final String url = "http://temp.com";
        when(jwt.getIssuer()).thenReturn(new URL(url));
        when(lookupParameters.getToken()).thenReturn(jwt);
        when(krrClient.getPersonResource(any(), any()))
                .thenThrow(IllegalStateException.class)
                .thenThrow(IllegalStateException.class)
                .thenReturn(personResource);

        kontaktInfoService.getCitizenInfo(lookupParameters);

        verify(krrClient, times(3)).getPersonResource(any(), any());
    }
}
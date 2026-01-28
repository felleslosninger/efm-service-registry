package no.difi.meldingsutveksling.serviceregistry.healthcare;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.http.Fault;
import no.difi.meldingsutveksling.serviceregistry.exceptions.ClientInputException;
import no.difi.meldingsutveksling.serviceregistry.exceptions.EntityNotFoundException;
import no.difi.meldingsutveksling.serviceregistry.exceptions.ServiceRegistryException;
import no.difi.meldingsutveksling.serviceregistry.record.LookupParameters;
import no.difi.meldingsutveksling.serviceregistry.service.healthcare.AddressRegistrerDetails;
import no.difi.meldingsutveksling.serviceregistry.service.healthcare.NhnService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.security.access.AccessDeniedException;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NhnServiceTest {

    private static final String NHN_URI = "http://localhost:8089/arlookup/{identifier}";
    private static final String IDENTIFIER = "123456789";
    private static final String TOKEN = "test-token";
    private static final String PATH = "/arlookup/" + IDENTIFIER;

    private WireMockServer wireMockServer;
    private NhnService nhnService;
    @Mock
    private LookupParameters lookupParameters;
    @Mock
    private Jwt jwt;
    private RestClient restClient;

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(options().port(8089));
        wireMockServer.start();
        WireMock.configureFor("localhost", 8089);

        restClient = RestClient.create();
        nhnService = new NhnService(NHN_URI,restClient);

        when(lookupParameters.getIdentifier()).thenReturn(IDENTIFIER);
        when(lookupParameters.getToken()).thenReturn(jwt);
        when(jwt.getTokenValue()).thenReturn(TOKEN);
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }

    @Test
    void testGetARDetailsSuccess() {
        String responseBody = """
            {
                "herid1": "67676",
                "herid2": "33232",
                "derDigdirSertifikat": "pem-cert-data",
                "ediAdress": "dummyedi@edi.edi",
                "orgNumber": "797897978"
            }
            """;
        wireMockServer.stubFor(get(urlEqualTo(PATH))
                .withHeader(HttpHeaders.AUTHORIZATION, equalTo("Bearer " + TOKEN))
                .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON_VALUE))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(responseBody)));

        AddressRegistrerDetails result = nhnService.getARDetails(lookupParameters);

        assertNotNull(result);
        assertEquals("67676", result.getHerid1());
        assertEquals("33232", result.getHerid2());
        assertEquals("pem-cert-data", result.getDerDigdirSertifikat());
        assertEquals("dummyedi@edi.edi", result.getEdiAdress());
        assertEquals("797897978", result.getOrgNumber());
        verify(getRequestedFor(urlEqualTo(PATH))
                .withHeader(HttpHeaders.AUTHORIZATION, equalTo( "Bearer " + TOKEN))
                .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON_VALUE)));
    }

    @Test
    void testGetARDetailsNotFound() {
        wireMockServer.stubFor(get(urlEqualTo(PATH))
                .withHeader(HttpHeaders.AUTHORIZATION, equalTo("Bearer " + TOKEN))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.NOT_FOUND.value())));

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> nhnService.getARDetails(lookupParameters));
        assertEquals(new EntityNotFoundException(lookupParameters.getIdentifier()).getMessage(), exception.getMessage());
        verify(getRequestedFor(urlEqualTo(PATH))
                .withHeader(HttpHeaders.AUTHORIZATION, equalTo("Bearer " + TOKEN)));
    }

    @Test
    void testGetARDetails_Unauthorized() {
        wireMockServer.stubFor(get(urlEqualTo(PATH))
                .withHeader(HttpHeaders.AUTHORIZATION, equalTo("Bearer " + TOKEN))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.UNAUTHORIZED.value())));

        AccessDeniedException exception = assertThrows(AccessDeniedException.class,
                () -> nhnService.getARDetails(lookupParameters));
        assertTrue(exception.getMessage().contains("AR lookup Access denied"));
        verify(getRequestedFor(urlEqualTo(PATH))
                .withHeader(HttpHeaders.AUTHORIZATION, equalTo("Bearer " + TOKEN)));
    }

    @Test
    void testGetARDetails_ClientError() {
        wireMockServer.stubFor(get(urlEqualTo(PATH))
                .withHeader(HttpHeaders.AUTHORIZATION, equalTo("Bearer " + TOKEN))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.BAD_REQUEST.value())));

        ClientInputException exception = assertThrows(ClientInputException.class,
                () -> nhnService.getARDetails(lookupParameters));
        assertEquals("Client input error for identifier" + IDENTIFIER, exception.getMessage());
        verify(getRequestedFor(urlEqualTo(PATH))
                .withHeader(HttpHeaders.AUTHORIZATION, equalTo("Bearer " + TOKEN)));
    }

    @Test
    void testGetARDetails_ServerError() {
        wireMockServer.stubFor(get(urlEqualTo(PATH))
                .withHeader(HttpHeaders.AUTHORIZATION, equalTo("Bearer " + TOKEN))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())));

        ServiceRegistryException exception = assertThrows(ServiceRegistryException.class,
                () -> nhnService.getARDetails(lookupParameters));
        assertEquals("Internal server fail while getting AR details for id " + IDENTIFIER, exception.getMessage());
        verify(getRequestedFor(urlEqualTo(PATH))
                .withHeader(HttpHeaders.AUTHORIZATION, equalTo("Bearer " + TOKEN)));
    }

    @Test
    void testGetARDetails_ResourceAccessException() {
        wireMockServer.stubFor(get(urlEqualTo(PATH))
                .withHeader(HttpHeaders.AUTHORIZATION, equalTo("Bearer " + TOKEN))
                .willReturn(aResponse()
                        .withFault(Fault.RANDOM_DATA_THEN_CLOSE)));

        ResourceAccessException exception = assertThrows(ResourceAccessException.class,
                () -> nhnService.getARDetails(lookupParameters));
        assertTrue(exception.getMessage().startsWith("I/O error on GET request for"));
        verify(getRequestedFor(urlEqualTo(PATH))
                .withHeader(HttpHeaders.AUTHORIZATION, equalTo("Bearer " + TOKEN)));
    }
}

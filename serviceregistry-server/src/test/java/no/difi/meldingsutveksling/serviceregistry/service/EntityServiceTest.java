package no.difi.meldingsutveksling.serviceregistry.service;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import no.difi.meldingsutveksling.serviceregistry.CacheConfig;
import no.difi.meldingsutveksling.serviceregistry.SRRequestScope;
import no.difi.meldingsutveksling.serviceregistry.client.brreg.BrregClient;
import no.difi.meldingsutveksling.serviceregistry.config.ServiceregistryProperties;
import no.difi.meldingsutveksling.serviceregistry.domain.BrregEnhet;
import no.difi.meldingsutveksling.serviceregistry.domain.BrregOrganisasjonsform;
import no.difi.meldingsutveksling.serviceregistry.domain.CitizenInfo;
import no.difi.meldingsutveksling.serviceregistry.domain.EntityInfo;
import no.difi.meldingsutveksling.serviceregistry.domain.FiksIoInfo;
import no.difi.meldingsutveksling.serviceregistry.domain.HelseEnhetInfo;
import no.difi.meldingsutveksling.serviceregistry.domain.OrganizationInfo;
import no.difi.meldingsutveksling.serviceregistry.exceptions.ClientInputException;
import no.difi.meldingsutveksling.serviceregistry.fiks.io.FiksIoService;
import no.difi.meldingsutveksling.serviceregistry.service.brreg.BrregNotFoundException;
import no.difi.meldingsutveksling.serviceregistry.service.brreg.BrregService;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import no.difi.meldingsutveksling.serviceregistry.service.healthcare.NhnServiceConfig;
import no.ks.fiks.io.client.model.FiksOrgId;
import no.ks.fiks.io.client.model.Konto;
import no.ks.fiks.io.client.model.KontoId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.when;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockReset;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.net.URI;
import java.util.Optional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,classes = {EntityService.class, CacheConfig.class, NhnServiceConfig.class})
@EnableConfigurationProperties(ServiceregistryProperties.class)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class EntityServiceTest {

    @MockitoBean
    private BrregClient brregClient;

    @MockitoBean(reset = MockReset.BEFORE)
    private BrregService brregService;

    @MockitoBean
    private ObjectProvider<FiksIoService> fiksIoServiceProvider;

    @MockitoBean
    private FiksIoService fiksIoService;

    @MockitoBean
    private Jwt jwt;

    @MockitoBean
    private SRRequestScope requestScope;

    @Autowired
    private EntityService entityService;
    @Autowired
    private ServiceregistryProperties serviceregistryProperties;

    @Test
    public void whenIdentifierIsCitizen_thenCitizenEntityIsReturned() {
        String FNR = "21905297101";
        Optional<EntityInfo> ei = entityService.getEntityInfo(FNR);
        assertTrue(ei.isPresent());
        assertEquals(CitizenInfo.class, ei.get().getClass());
        assertEquals(FNR, ei.get().getIdentifier());
    }

    @Test
    public void whenIdentifierIsUUID_thenEmptyIfFiksIONotEnabled() {
        String UUID = java.util.UUID.randomUUID().toString();
        when(fiksIoServiceProvider.getIfAvailable()).thenReturn(null);
        Optional<EntityInfo> ei = entityService.getEntityInfo(UUID);
        assertTrue(ei.isEmpty());


    }

    @Test
    public void whenIdentifierIsUUID_thenFiksIoInfoIsReturned() {

        String UUID = java.util.UUID.randomUUID().toString();
        when(fiksIoServiceProvider.getIfAvailable()).thenReturn(fiksIoService);
        when(fiksIoService.lookup(UUID)).thenReturn(Optional.of(Konto.builder().kontoId(new KontoId(java.util.UUID.randomUUID())).isGyldigMottaker(true).kontoNavn("dummynavn").fiksOrgId(new FiksOrgId(java.util.UUID.randomUUID())).fiksOrgNavn("dummynavn").build()));
        Optional<EntityInfo> ei = entityService.getEntityInfo(UUID);
        assertTrue(ei.isPresent());
        assertEquals(FiksIoInfo.class, ei.get().getClass());

    }

    @Test
    public void whenIdentifierOrgnumber_thenBregEnhetIsReturned() {
        String identifier = "920640818";
        try {
            when(brregService.getOrganizationInfo(identifier)).thenReturn(Optional.of( new BrregEnhet().setOrganisasjonsnummer(identifier).setNavn("dummy organisation").setOrganisasjonsform(new BrregOrganisasjonsform("AS"))).map(OrganizationInfo::of));
            Optional<EntityInfo> ei = entityService.getEntityInfo(identifier);
            assertTrue(ei.isPresent());
            assertEquals(OrganizationInfo.class, ei.get().getClass());
            assertEquals(identifier, ei.get().getIdentifier());

        } catch (BrregNotFoundException e) {
            fail("It was supposed to return result");
        }

    }

    @Test
    public void whenIdentifierOrgnumberNotFound_thenEmptyResult() {
        String identifier = "920640818";
        try {
            when(brregService.getOrganizationInfo(identifier)).thenThrow(new BrregNotFoundException(identifier));
            Optional<EntityInfo> ei = entityService.getEntityInfo(identifier);
            assertTrue(ei.isEmpty());

        } catch (BrregNotFoundException e) {
            fail("It was supposed to return empty");
        }
    }

    @Test
    public void whenIdentifierIsNhn_thenHelseEnhetIsReturned() {
        var HERID2 = "33232";
        WireMockServer wireMockServer = new WireMockServer(options().port(8089));
        wireMockServer.start();
        WireMock.configureFor("localhost", 8089);

        String responseBody = """
            {
                "herid1": "67676",
                "herid2": "{herId2}",
                "pemDigdirSertifikat": "pem-cert-data",
                "ediAdress": "dummyedi@edi.edi",
                "orgNumber": "797897978"
            }
            """.replace("{herId2}",HERID2);
        String dummyToken = "dummy-token";
        String path = URI.create(serviceregistryProperties.getHealthcare().nhnAdapterEndpointUrl().replace("{identifier}", "33232"))
                .getPath();
        wireMockServer.stubFor(get(path)
                .withHeader(HttpHeaders.AUTHORIZATION, equalTo("Bearer " + dummyToken))
                .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON_VALUE))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(responseBody)));

        when(jwt.getTokenValue()).thenReturn(dummyToken);
        when(requestScope.getToken()).thenReturn(jwt);
        Optional<EntityInfo> ei = entityService.getEntityInfo(HERID2);
        assertTrue(ei.isPresent());
        assertEquals(HelseEnhetInfo.class, ei.get().getClass());
        wireMockServer.stop();


    }

    @Test
    public void whenIdentifierIsNhn_And_Arlookup404_thenEmptyResult() {
        WireMockServer wireMockServer = new WireMockServer(options().port(8089));
        wireMockServer.start();
        WireMock.configureFor("localhost", 8089);

        String dummyToken = "dummy-token";
        String path = URI.create(serviceregistryProperties.getHealthcare().nhnAdapterEndpointUrl().replace("{identifier}", "33232"))
                .getPath();
        wireMockServer.stubFor(get(path)
                .withHeader(HttpHeaders.AUTHORIZATION, equalTo("Bearer " + dummyToken))
                .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON_VALUE))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.NOT_FOUND.value())));

        when(jwt.getTokenValue()).thenReturn(dummyToken);
        when(requestScope.getToken()).thenReturn(jwt);
        Optional<EntityInfo> ei = entityService.getEntityInfo("33232");
        assertTrue(ei.isEmpty());
        wireMockServer.stop();


    }


    @Test
    public void whenIdentifierIsNhn_And_ArlookupThrowsException_thenExceptionIsThrown() {
        WireMockServer wireMockServer = new WireMockServer(options().port(8089));
        wireMockServer.start();
        WireMock.configureFor("localhost", 8089);

        String dummyToken = "dummy-token";
        String path = URI.create(serviceregistryProperties.getHealthcare().nhnAdapterEndpointUrl().replace("{identifier}", "33232"))
                .getPath();
        wireMockServer.stubFor(get(path)
                .withHeader(HttpHeaders.AUTHORIZATION, equalTo("Bearer " + dummyToken))
                .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON_VALUE))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.BAD_REQUEST.value())));

        when(jwt.getTokenValue()).thenReturn(dummyToken);
        when(requestScope.getToken()).thenReturn(jwt);
        assertThrows(ClientInputException.class, () -> entityService.getEntityInfo("33232"));
        wireMockServer.stop();


    }

    @Test
    public void whenIdentifierIsNhn_And_ArlookupError400_thenClientInputExceptionIsThrown() {
        WireMockServer wireMockServer = new WireMockServer(options().port(8089));
        wireMockServer.start();
        WireMock.configureFor("localhost", 8089);

        String dummyToken = "dummy-token";
        String path = URI.create(serviceregistryProperties.getHealthcare().nhnAdapterEndpointUrl().replace("{identifier}", "33232"))
                .getPath();
        wireMockServer.stubFor(get(path)
                .withHeader(HttpHeaders.AUTHORIZATION, equalTo("Bearer " + dummyToken))
                .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON_VALUE))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.BAD_REQUEST.value())));

        when(jwt.getTokenValue()).thenReturn(dummyToken);
        when(requestScope.getToken()).thenReturn(jwt);
        assertThrows(ClientInputException.class, () -> entityService.getEntityInfo("33232"));
        wireMockServer.stop();

    }

    @Test
    public void whenIdentifierIsNhn_And_ArlookupError404_thenEmptyResult() {
        WireMockServer wireMockServer = new WireMockServer(options().port(8089));
        wireMockServer.start();
        WireMock.configureFor("localhost", 8089);

        String dummyToken = "dummy-token";
        String path = URI.create(serviceregistryProperties.getHealthcare().nhnAdapterEndpointUrl().replace("{identifier}", "33232"))
                .getPath();
        wireMockServer.stubFor(get(path)
                .withHeader(HttpHeaders.AUTHORIZATION, equalTo("Bearer " + dummyToken))
                .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON_VALUE))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.NOT_FOUND.value())));

        when(jwt.getTokenValue()).thenReturn(dummyToken);
        when(requestScope.getToken()).thenReturn(jwt);
        assertTrue(entityService.getEntityInfo("33232").isEmpty());
        wireMockServer.stop();


    }

}

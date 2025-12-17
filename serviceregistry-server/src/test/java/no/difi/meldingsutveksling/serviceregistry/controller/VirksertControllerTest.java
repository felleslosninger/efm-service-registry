package no.difi.meldingsutveksling.serviceregistry.controller;

import no.difi.meldingsutveksling.serviceregistry.CertificateNotFoundException;
import no.difi.meldingsutveksling.serviceregistry.SRRequestScope;
import no.difi.meldingsutveksling.serviceregistry.config.SRConfig;
import no.difi.meldingsutveksling.serviceregistry.security.PayloadSigner;
import no.difi.meldingsutveksling.serviceregistry.service.AuthenticationService;
import no.difi.meldingsutveksling.serviceregistry.service.virksert.VirkSertService;
import no.difi.virksert.client.lang.VirksertClientException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation;
import org.springframework.restdocs.operation.preprocess.Preprocessors;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;

@ExtendWith(SpringExtension.class)
@WebMvcTest(VirksertController.class)
@ContextConfiguration(classes = {VirksertController.class, GlobalControllerExceptionHandler.class, SRConfig.class})
@ActiveProfiles("test")
@AutoConfigureRestDocs
@AutoConfigureMockMvc()
public class VirksertControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private PayloadSigner payloadSigner;

    @MockitoBean
    private SRRequestScope requestScope;

    @MockitoBean
    private VirkSertService virksertService;

    @MockitoBean
    private AuthenticationService authenticationService;

    @MockitoBean
    private Jwt jwtMock;

    @Mock
    private Authentication authMock;

    @BeforeEach
    public void before() throws CertificateNotFoundException {
        when(authMock.isAuthenticated()).thenReturn(true);
        when(authMock.getPrincipal()).thenReturn(true);
        when(authMock.getName()).thenReturn(null);

        when(requestScope.getIdentifier()).thenReturn("910076787");
        when(requestScope.getConversationId()).thenReturn("90c0d1a0-889b-4cee-b885-14839c81b411");
        when(requestScope.getClientId()).thenReturn("36320099-610a-443c-ad0d-555829ff013c");

        when(virksertService.getCertificate(eq("910076787"), any()))
                .thenReturn("pem123");

        when(virksertService.getCertificate(eq("123123123"), any()))
                .thenThrow(new CertificateNotFoundException("",new VirksertClientException("not found")));

        when(jwtMock.getClaims()).thenReturn(Map.of("scope", "move/dpo.read move/dpe.read"));
        when(authenticationService.getToken(any())).thenReturn(jwtMock);
    }

    @Test
    public void testControllerShouldReturnCertificate() throws Exception {
        mvc.perform(
                get("/virksert/{identifier}", "910076787")
                        .accept(MediaType.TEXT_PLAIN_VALUE)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(authMock))
        )
                .andExpect(status().isOk())
                .andDo(print())
                .andExpect(content().string("pem123"))
                .andDo(MockMvcRestDocumentation.document(
                        "virksert/get",
                        Preprocessors.preprocessRequest(Preprocessors.prettyPrint()),
                        Preprocessors.preprocessResponse(Preprocessors.prettyPrint())
                ));
    }

    @Test
    public void testControllerShouldReturnError() throws Exception {
        mvc.perform(
                get("/virksert/{identifier}", "123123123")
                        .accept(MediaType.TEXT_PLAIN_VALUE)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(authMock))
        )
                .andExpect(status().isNotFound())
                .andDo(print())
                .andDo(MockMvcRestDocumentation.document(
                        "virksert/notfound",
                        Preprocessors.preprocessRequest(Preprocessors.prettyPrint()),
                        Preprocessors.preprocessResponse(Preprocessors.prettyPrint())
                ));
    }
}


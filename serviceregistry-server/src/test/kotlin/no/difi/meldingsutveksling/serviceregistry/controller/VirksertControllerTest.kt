package no.difi.meldingsutveksling.serviceregistry.controller

import com.ninjasquad.springmockk.MockkBean
import com.ninjasquad.springmockk.SpykBean
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.mockk
import no.difi.meldingsutveksling.domain.ICD
import no.difi.meldingsutveksling.domain.Iso6523
import no.difi.meldingsutveksling.serviceregistry.SRRequestScope
import no.difi.meldingsutveksling.serviceregistry.config.SRConfig
import no.difi.meldingsutveksling.serviceregistry.security.PayloadSigner
import no.difi.meldingsutveksling.serviceregistry.service.AuthenticationService
import no.difi.meldingsutveksling.serviceregistry.service.virksert.VirkSertService
import no.difi.virksert.client.lang.VirksertClientException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get
import org.springframework.restdocs.operation.preprocess.Preprocessors.*
import org.springframework.security.core.Authentication
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@ExtendWith(SpringExtension::class)
@WebMvcTest(VirksertController::class)
@Import(value = [SRConfig::class, GlobalControllerExceptionHandler::class])
@TestPropertySource("classpath:application-test.properties")
@WithMockUser
@AutoConfigureRestDocs
class VirksertControllerTest {

    @Autowired
    lateinit var mvc: MockMvc

    @MockkBean
    lateinit var payloadSigner: PayloadSigner

    @SpykBean
    lateinit var requestScope: SRRequestScope

    @MockkBean
    lateinit var virksertService: VirkSertService

    @MockkBean
    lateinit var authenticationService: AuthenticationService

    private val authMock = mockk<Authentication> {
        every { isAuthenticated } returns true
        every { principal } returns true
        every { name } returns null
    }

    @BeforeEach
    fun before() {
        MockKAnnotations.init(this)

        with(requestScope) {
            every { identifier } returns Iso6523.of(ICD.NO_ORG, "910076787")
            every { conversationId } returns "90c0d1a0-889b-4cee-b885-14839c81b411"
            every { clientId } returns Iso6523.of(ICD.NO_ORG, "123123123")
            every { isUsePlainFormat } returns false
        }

        with(virksertService) {
            every { getCertificate(any(), any()) } throws VirksertClientException("not found")
            every { getCertificate(Iso6523.of(ICD.NO_ORG, "910076787"), any()) } returns "pem123"
        }

        every { authenticationService.getToken(any()) } returns mockk {
            every { claims } returns mapOf("scope" to "move/dpo.read move/dpe.read")
        }
    }

    @Test
    fun `test controller should return certificate`() {
        mvc.perform(get("/virksert/{identifier}", "910076787")
                        .accept(MediaType.TEXT_PLAIN_VALUE)
                        .with(authentication(authMock)))
            .andExpect(status().isOk)
            .andDo(print())
            .andExpect(content().string("pem123"))
            .andDo(
                document(
                    "virksert/get",
                    preprocessRequest(prettyPrint()),
                    preprocessResponse(prettyPrint())
                )
            )
    }

    @Test
    fun `test controller should return error`() {
        mvc.perform(get("/virksert/{identifier}", "123123123")
                        .accept(MediaType.TEXT_PLAIN_VALUE)
                        .with(authentication(authMock)))
            .andExpect(status().isNotFound)
            .andDo(print())
            .andDo(
                document(
                    "virksert/notfound",
                    preprocessRequest(prettyPrint()),
                    preprocessResponse(prettyPrint())
                )
            )
    }
}
package no.difi.meldingsutveksling.serviceregistry.controller

import com.ninjasquad.springmockk.MockkBean
import io.mockk.MockKAnnotations
import io.mockk.every
import no.difi.meldingsutveksling.serviceregistry.SRRequestScope
import no.difi.meldingsutveksling.serviceregistry.service.virksert.VirkSertService
import no.difi.virksert.client.lang.VirksertClientException
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get
import org.springframework.restdocs.operation.preprocess.Preprocessors.*
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@RunWith(SpringRunner::class)
@WebMvcTest(VirksertController::class)
@ContextConfiguration(classes = [VirksertController::class, GlobalControllerExceptionHandler::class])
@TestPropertySource("classpath:application-test.properties")
@AutoConfigureRestDocs
@AutoConfigureMockMvc(addFilters = false)
class VirksertControllerTest {

    @Autowired
    lateinit var mvc: MockMvc

    @MockkBean
    lateinit var requestScope: SRRequestScope

    @MockkBean
    lateinit var virksertService: VirkSertService

    @Before
    fun before() {
        MockKAnnotations.init(this)

        with(requestScope) {
            every { identifier } returns "910076787"
            every { conversationId } returns "90c0d1a0-889b-4cee-b885-14839c81b411"
            every { clientId } returns "36320099-610a-443c-ad0d-555829ff013c"
        }

        with(virksertService) {
            every { getCertificate(any()) } throws VirksertClientException("not found")
            every { getCertificate("910076787") } returns "pem123"
        }
    }

    @Test
    fun `test controller should return certificate`() {
        mvc.perform(get("/virksert/{identifier}", "910076787"))
            .andExpect(status().isOk)
            .andDo(print())
            .andDo(MockMvcRestDocumentation.document("virksert/get",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint())
            ))
    }

    @Test
    fun `test controller should return error`() {
        mvc.perform(get("/virksert/{identifier}", "123123123"))
            .andExpect(status().isNotFound)
            .andDo(print())
            .andDo(MockMvcRestDocumentation.document("virksert/notfound",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint())
            ))
    }
}
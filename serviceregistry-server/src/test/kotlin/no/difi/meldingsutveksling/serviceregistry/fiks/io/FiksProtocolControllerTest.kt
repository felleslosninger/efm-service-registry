package no.difi.meldingsutveksling.serviceregistry.fiks.io

import com.ninjasquad.springmockk.MockkBean
import io.mockk.*
import org.hamcrest.Matchers
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*
import org.springframework.restdocs.operation.preprocess.Preprocessors.*
import org.springframework.restdocs.request.ParameterDescriptor
import org.springframework.restdocs.request.RequestDocumentation
import org.springframework.restdocs.request.RequestDocumentation.pathParameters
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.*

@RunWith(SpringRunner::class)
@ContextConfiguration(classes = [FiksProtocolController::class])
@TestPropertySource("classpath:application-test.properties")
@WebMvcTest(value = [FiksProtocolController::class])
@AutoConfigureRestDocs
@AutoConfigureMockMvc(addFilters = false)
class FiksProtocolControllerTest {

    @Autowired
    lateinit var mvc: MockMvc

    @MockkBean
    lateinit var fiksProtolRepository: FiksProtocolRepository

    @Before
    fun before() {
        MockKAnnotations.init(this)

        val fooProtocol = FiksProtocol(0, "foo.bar", mutableSetOf())

        with(fiksProtolRepository) {
            every { findByIdentifier("foo.bar") } returns fooProtocol
            justRun { deleteByIdentifier("foo.bar") }
            every { save(any<FiksProtocol>()) } returns fooProtocol
            every { findAll() } returns listOf(fooProtocol)
        }

    }

    @Test
    fun `test add protocol`() {
        mvc.perform(post("/api/v1/fiks/{identifier}", "foo.bar"))
                .andExpect(status().isOk)
                .andDo(print())
                .andDo(document("fiks/post",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint())
                ))
    }

    @Test
    fun `test delete protocol`() {
        mvc.perform(delete("/api/v1/fiks/{identifier}", "foo.bar"))
                .andExpect(status().isOk)
                .andDo(document("fiks/delete",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(identifierParam())
                ))
    }

    @Test
    fun `test list protocols`() {
        mvc.perform(get("/api/v1/fiks"))
                .andExpect(status().isOk)
                .andDo(print())
                .andExpect(jsonPath("$[0].identifier", Matchers.`is`("foo.bar")))
                .andDo(document("fiks/get",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint())
                ))
    }

    @Test
    fun `test find by identifier`() {
        mvc.perform(get("/api/v1/fiks/{identifier}", "foo.bar"))
                .andExpect(status().isOk)
                .andDo(print())
                .andExpect(jsonPath("$.identifier", Matchers.`is`("foo.bar")))
                .andDo(document("fiks/findby",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(identifierParam())
                ))

    }

    private fun identifierParam(): ParameterDescriptor {
        return RequestDocumentation.parameterWithName("identifier").description("Identifier of FIKS protocol")
    }

}
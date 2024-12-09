package no.difi.meldingsutveksling.serviceregistry.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import no.difi.meldingsutveksling.serviceregistry.controller.AdminController;
import no.difi.meldingsutveksling.serviceregistry.domain.DocumentType;
import no.difi.meldingsutveksling.serviceregistry.domain.Process;
import no.difi.meldingsutveksling.serviceregistry.domain.ProcessCategory;
import no.difi.meldingsutveksling.serviceregistry.service.DocumentTypeService;
import no.difi.meldingsutveksling.serviceregistry.service.ProcessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.http.MockHttpOutputMessage;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.restdocs.request.ParameterDescriptor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(value = AdminController.class)
@ContextConfiguration(classes = AdminController.class)
@ActiveProfiles("itest")
@AutoConfigureRestDocs
@AutoConfigureMockMvc(addFilters = false)
public class AdminControllerTest {

    private static final String PROCESSES_ENDPOINT_PATH = "/api/v1/processes";
    private static final String DOCUMENT_TYPES_ENDPOINT_PATH = "/api/v1/documentTypes";
    private static final URI DOCUMENT_TYPES_ENDPOINT_URI = UriComponentsBuilder.fromUriString(DOCUMENT_TYPES_ENDPOINT_PATH).build().toUri();
    private static final URI PROCESSES_ENDPOINT_URI = UriComponentsBuilder.fromUriString(PROCESSES_ENDPOINT_PATH).build().toUri();

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProcessService processServiceMock;

    @MockBean
    private DocumentTypeService documentTypeServiceMock;

    private HttpMessageConverter<Object> messageConverter;

    @BeforeEach
    public void setUp() {
        ObjectMapper objectMapper = new ObjectMapper().disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        messageConverter = new MappingJackson2HttpMessageConverter(objectMapper);
    }

    @Test
    public void addProcess_EmptyRequestBody_ResponseShouldBeBadRequest() throws Exception {
        MockHttpServletResponse response = doPost(PROCESSES_ENDPOINT_URI, null);
        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatus());
    }

    @Test
    public void addProcess_OccupiedIdentifier_ResponseShouldBeConflict() throws Exception {
        when(processServiceMock.findByIdentifier(anyString())).thenReturn(Optional.of(mock(Process.class)));
        Process process = createProcess("ProcessIdentifier", ProcessCategory.ARKIVMELDING, "code", "editionCode", new ArrayList<>());

        MockHttpServletResponse response = doPost(PROCESSES_ENDPOINT_URI, process);

        assertEquals(HttpStatus.CONFLICT.value(), response.getStatus());
    }

    @Test
    public void addProcess_Success_ResponseShouldBeCreated() throws Exception {
        when(processServiceMock.findByIdentifier(anyString())).thenReturn(Optional.empty());
        when(documentTypeServiceMock.findByIdentifier(anyString())).thenReturn(Optional.empty());
        ArrayList<DocumentType> documentTypes = new ArrayList<>();
        documentTypes.add(createDocumentType("DocumentTypeIdentifier"));
        Process process = createProcess("ProcessIdentifier", ProcessCategory.ARKIVMELDING, "code", "editionCode", documentTypes);

        MockHttpServletResponse response = mockMvc.perform(post(PROCESSES_ENDPOINT_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .content(getJson(process)))
                .andDo(print())
                .andDo(document("admin/processes/post",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint())
                ))
                .andReturn().getResponse();

        assertEquals(HttpStatus.CREATED.value(), response.getStatus());
        verify(processServiceMock).add(any(Process.class));
        verify(documentTypeServiceMock).add(any(DocumentType.class));
    }

    @Test
    public void getProcess_ProcessNotFound_ResponseShouldBeNotFound() throws Exception {
        when(processServiceMock.findByIdentifier(anyString())).thenReturn(Optional.empty());
        URI processUri = UriComponentsBuilder.fromUri(PROCESSES_ENDPOINT_URI)
                .pathSegment("ProcessIdentifier").build().toUri();

        mockMvc.perform(get(processUri)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    public void getProcess_ProcessFound_ResponseShouldBeOk() throws Exception {
        String processIdentifier = "urn:no:difi:profile:foo:ver1.0";
        Process process = createProcess(processIdentifier, ProcessCategory.ARKIVMELDING, "code", "editionCode", new ArrayList<>());
        when(processServiceMock.findByIdentifier(anyString())).thenReturn(Optional.of(process));

        mockMvc.perform(get(PROCESSES_ENDPOINT_PATH+"/{identifier}", processIdentifier)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(print())
                .andDo(document("admin/processes/get",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(getIdentifierParam())
                ));
    }

    @Test
    public void getProcesses_Success_ResponseShouldBeOk() throws Exception {
        mockMvc.perform(get(PROCESSES_ENDPOINT_URI)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(print())
                .andDo(document("admin/processes/getall",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint())
                ));
        verify(processServiceMock).findAll();
    }

    @Test
    public void deleteProcess_ProcessNotFound_ResponseShouldBeNotFound() throws Exception {
        when(processServiceMock.findByIdentifier(anyString())).thenReturn(Optional.empty());
        URI processUri = UriComponentsBuilder.fromUri(PROCESSES_ENDPOINT_URI)
                .pathSegment("NoProcess").build().toUri();

        MockHttpServletResponse response = doDelete(processUri);

        assertEquals(HttpStatus.NOT_FOUND.value(), response.getStatus());
    }

    @Test
    public void deleteProcess_ProcessFound_ResponseShouldBeNoContent() throws Exception {
        Process processToDelete = mock(Process.class);
        when(processServiceMock.findByIdentifier(anyString())).thenReturn(Optional.of(processToDelete));

        mockMvc.perform(delete(PROCESSES_ENDPOINT_PATH+"/{identifier}", "urn:no:difi:profile:foo:ver1.0"))
                .andDo(print())
                .andExpect(status().isNoContent())
                .andDo(document("admin/processes/delete",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(getIdentifierParam())
                ));

        verify(processServiceMock).delete(processToDelete);
    }

    @Test
    public void updateProcess_WithoutProcessIdentifier_ResponseShouldBeMethodNotAllowed() throws Exception {
        List<String> documentTypeIds = new ArrayList<>();
        documentTypeIds.add("Document1");

        MockHttpServletResponse response = doPut(PROCESSES_ENDPOINT_URI, documentTypeIds);

        assertEquals(HttpStatus.METHOD_NOT_ALLOWED.value(), response.getStatus());
    }

    @Test
    public void updateProcess_Success_ResponseShouldBeOk() throws Exception {
        URI processUri = UriComponentsBuilder.fromUri(PROCESSES_ENDPOINT_URI).pathSegment("processID").build().toUri();
        Process updatedValues = createProcess("n/a", null, "serviceCode2", null, null);
        when(processServiceMock.update(anyString(), any(Process.class))).thenReturn(true);

        mockMvc.perform(put(processUri)
                .contentType(MediaType.APPLICATION_JSON)
                .content(getJson(updatedValues)))
                .andExpect(status().isOk())
                .andDo(print())
                .andDo(document("admin/processes/put",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint())
                ));
    }

    @Test
    public void updateProcess_Failure_ResponseShouldBeNotFound() throws Exception {
        URI processUri = UriComponentsBuilder.fromUri(PROCESSES_ENDPOINT_URI).pathSegment("processID").build().toUri();
        Process updatedValues = createProcess("n/a", null, "serviceCode2", null, null);
        when(processServiceMock.update(anyString(), any(Process.class))).thenReturn(false);

        MockHttpServletResponse response = doPut(processUri, updatedValues);

        assertEquals(HttpStatus.NOT_FOUND.value(), response.getStatus());
    }

    @Test
    public void addDocumentType_EmptyRequestBody_ResponseShouldBeBadRequest() throws Exception {
        MockHttpServletResponse response = doPost(DOCUMENT_TYPES_ENDPOINT_URI, null);
        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatus());
    }

    @Test
    public void addDocumentType_OccupiedIdentifier_ResponseShouldBeConflict() throws Exception {
        when(documentTypeServiceMock.findByIdentifier(anyString())).thenReturn(Optional.of(mock(DocumentType.class)));
        DocumentType documentType = createDocumentType("DocumentTypeID");

        MockHttpServletResponse response = doPost(DOCUMENT_TYPES_ENDPOINT_URI, documentType);

        assertEquals(HttpStatus.CONFLICT.value(), response.getStatus());
    }

    @Test
    public void addDocumentType_Success_ResponseShouldBeCreated() throws Exception {
        when(documentTypeServiceMock.findByIdentifier(anyString())).thenReturn(Optional.empty());
        DocumentType documentType = createDocumentType("DocumentTypeID");

        mockMvc.perform(post(DOCUMENT_TYPES_ENDPOINT_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .content(getJson(documentType)))
                .andExpect(status().isCreated())
                .andDo(print())
                .andDo(document("admin/documenttypes/post",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint())
                ));

        verify(documentTypeServiceMock).add(any(DocumentType.class));
    }

    @Test
    public void getDocumentType_DocumentTypeNotFound_ResponseShouldBeNotFound() throws Exception {
        when(documentTypeServiceMock.findByIdentifier(anyString())).thenReturn(Optional.empty());
        URI uri = UriComponentsBuilder.fromUri(DOCUMENT_TYPES_ENDPOINT_URI)
                .pathSegment("ProcessIdentifier").build().toUri();

        MockHttpServletResponse response = doGet(uri);

        assertEquals(HttpStatus.NOT_FOUND.value(), response.getStatus());
    }

    @Test
    public void getDocumentType_DocumentTypeFound_ResponseShouldBeOk() throws Exception {
        String docIdentifier = "urn:no:difi:foo:xsd::foo";
        DocumentType documentType = createDocumentType(docIdentifier);
        when(documentTypeServiceMock.findByIdentifier(anyString())).thenReturn(Optional.of(documentType));

        mockMvc.perform(get(DOCUMENT_TYPES_ENDPOINT_PATH+"/{identifier}", docIdentifier)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(print())
                .andDo(document("admin/documenttypes/get",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(getIdentifierParam())
                ));
    }

    @Test
    public void getDocumentTypes_Success_ResponseShouldBeOk() throws Exception {
        mockMvc.perform(get(DOCUMENT_TYPES_ENDPOINT_URI)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(print())
                .andDo(document("admin/documenttypes/getall",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint())
                ));
        verify(documentTypeServiceMock).findAll();
    }

    @Test
    public void deleteDocumentType_DocumentTypeNotFound_ResponseShouldBeNotFound() throws Exception {
        when(documentTypeServiceMock.findByIdentifier(anyString())).thenReturn(Optional.empty());
        URI uri = UriComponentsBuilder.fromUri(DOCUMENT_TYPES_ENDPOINT_URI)
                .pathSegment("NoProcess").build().toUri();

        MockHttpServletResponse response = doDelete(uri);

        assertEquals(HttpStatus.NOT_FOUND.value(), response.getStatus());
    }

    @Test
    public void deleteDocumentType_DocumentTypeFound_ResponseShouldBeNoContent() throws Exception {
        DocumentType documentTypeToDelete = mock(DocumentType.class);
        when(documentTypeServiceMock.findByIdentifier(anyString())).thenReturn(Optional.of(documentTypeToDelete));

        mockMvc.perform(delete(DOCUMENT_TYPES_ENDPOINT_PATH+"/{identifier}", "urn:no:difi:foo:xsd::foo"))
                .andExpect(status().isNoContent())
                .andDo(print())
                .andDo(document("admin/documenttypes/delete",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(getIdentifierParam())
                ));

        verify(documentTypeServiceMock).delete(documentTypeToDelete);
    }

    private DocumentType createDocumentType(String identifier) {
        DocumentType documentType = new DocumentType();
        documentType.setIdentifier(identifier);
        return documentType;
    }

    private Process createProcess(String identifier, ProcessCategory category, String serviceCode, String serviceEditionCode, List<DocumentType> documentTypes) {
        Process process = new Process();
        process.setIdentifier(identifier);
        process.setServiceCode(serviceCode);
        process.setServiceEditionCode(serviceEditionCode);
        process.setCategory(category);
        process.setDocumentTypes(documentTypes);
        return process;
    }

    private MockHttpServletResponse doDelete(URI uri) throws Exception {
        return mockMvc.perform(delete(uri)).andReturn().getResponse();
    }

    private MockHttpServletResponse doGet(URI uri) throws Exception {
        return mockMvc.perform(get(uri)
                .accept(MediaType.APPLICATION_JSON))
                .andReturn().getResponse();
    }

    private MockHttpServletResponse doPut(URI uri, Object content) throws Exception {
        return mockMvc.perform(put(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .content(getJson(content)))
                .andReturn().getResponse();
    }

    private MockHttpServletResponse doPost(URI uri, Object content) throws Exception {
        return mockMvc.perform(post(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .content(getJson(content)))
                .andReturn().getResponse();
    }

    private ParameterDescriptor getIdentifierParam() {
        return parameterWithName("identifier").description("Identifier");
    }


    private String getJson(Object o) throws IOException {
        if (o == null) return "";
        MockHttpOutputMessage mockHttpOutputMessage = new MockHttpOutputMessage();
        messageConverter.write(o, MediaType.APPLICATION_JSON, mockHttpOutputMessage);
        return mockHttpOutputMessage.getBodyAsString();
    }

}

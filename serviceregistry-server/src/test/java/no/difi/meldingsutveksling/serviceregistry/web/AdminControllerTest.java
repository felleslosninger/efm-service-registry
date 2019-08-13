package no.difi.meldingsutveksling.serviceregistry.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import no.difi.meldingsutveksling.serviceregistry.model.DocumentType;
import no.difi.meldingsutveksling.serviceregistry.model.Process;
import no.difi.meldingsutveksling.serviceregistry.model.ProcessCategory;
import no.difi.meldingsutveksling.serviceregistry.service.DocumentTypeService;
import no.difi.meldingsutveksling.serviceregistry.service.ProcessService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.http.MockHttpOutputMessage;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@RunWith(MockitoJUnitRunner.class)
public class AdminControllerTest {

    private static final String PROCESSES_ENDPOINT_PATH = "/api/v1/processes";
    private static final String DOCUMENT_TYPES_ENDPOINT_PATH = "/api/v1/documentTypes";
    private static final URI DOCUMENT_TYPES_ENDPOINT_URI = UriComponentsBuilder.fromUriString(DOCUMENT_TYPES_ENDPOINT_PATH).build().toUri();
    private static final URI PROCESSES_ENDPOINT_URI = UriComponentsBuilder.fromUriString(PROCESSES_ENDPOINT_PATH).build().toUri();

    @InjectMocks
    private AdminController target;

    private MockMvc mockMvc;
    private HttpMessageConverter messageConverter;

    @Mock
    private ProcessService processServiceMock;
    @Mock
    private DocumentTypeService documentTypeServiceMock;

    @Before
    public void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(target).build();
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

        MockHttpServletResponse response = doPost(PROCESSES_ENDPOINT_URI, process);

        assertEquals(HttpStatus.CREATED.value(), response.getStatus());
        verify(processServiceMock).add(any(Process.class));
        verify(documentTypeServiceMock).add(any(DocumentType.class));
    }

    @Test
    public void getProcess_ProcessNotFound_ResponseShouldBeNotFound() throws Exception {
        when(processServiceMock.findByIdentifier(anyString())).thenReturn(Optional.empty());
        URI processUri = UriComponentsBuilder.fromUri(PROCESSES_ENDPOINT_URI)
                .pathSegment("ProcessIdentifier").build().toUri();

        MockHttpServletResponse response = doGet(processUri);

        assertEquals(HttpStatus.NOT_FOUND.value(), response.getStatus());
    }

    @Test
    public void getProcess_ProcessFound_ResponseShouldBeOk() throws Exception {
        Process process = createProcess("ProcessIdentifier", ProcessCategory.ARKIVMELDING, "code", "editionCode", new ArrayList<>());
        when(processServiceMock.findByIdentifier(anyString())).thenReturn(Optional.of(process));
        URI processUri = UriComponentsBuilder.fromUri(PROCESSES_ENDPOINT_URI)
                .pathSegment("ProcessIdentifier").build().toUri();

        MockHttpServletResponse response = doGet(processUri);

        assertEquals(HttpStatus.OK.value(), response.getStatus());
    }

    @Test
    public void getProcesses_Success_ResponseShouldBeOk() throws Exception {
        MockHttpServletResponse response = doGet(PROCESSES_ENDPOINT_URI);
        assertEquals(HttpStatus.OK.value(), response.getStatus());
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
        URI processUri = UriComponentsBuilder.fromUri(PROCESSES_ENDPOINT_URI)
                .pathSegment("Process").build().toUri();

        MockHttpServletResponse response = doDelete(processUri);

        assertEquals(HttpStatus.NO_CONTENT.value(), response.getStatus());
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

        MockHttpServletResponse response = doPut(processUri, updatedValues);

        assertEquals(HttpStatus.OK.value(), response.getStatus());
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

        MockHttpServletResponse response = doPost(DOCUMENT_TYPES_ENDPOINT_URI, documentType);

        assertEquals(HttpStatus.CREATED.value(), response.getStatus());
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
        DocumentType documentType = createDocumentType("identifier");
        when(documentTypeServiceMock.findByIdentifier(anyString())).thenReturn(Optional.of(documentType));
        URI uri = UriComponentsBuilder.fromUri(DOCUMENT_TYPES_ENDPOINT_URI)
                .pathSegment("ProcessIdentifier").build().toUri();

        MockHttpServletResponse response = doGet(uri);

        assertEquals(HttpStatus.OK.value(), response.getStatus());
    }

    @Test
    public void getDocumentTypes_Success_ResponseShouldBeOk() throws Exception {
        MockHttpServletResponse response = doGet(DOCUMENT_TYPES_ENDPOINT_URI);
        assertEquals(HttpStatus.OK.value(), response.getStatus());
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
        URI uri = UriComponentsBuilder.fromUri(DOCUMENT_TYPES_ENDPOINT_URI)
                .pathSegment("DocumentType").build().toUri();

        MockHttpServletResponse response = doDelete(uri);

        assertEquals(HttpStatus.NO_CONTENT.value(), response.getStatus());
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

    private String getJson(Object o) throws IOException {
        MockHttpOutputMessage mockHttpOutputMessage = new MockHttpOutputMessage();
        messageConverter.write(o, MediaType.APPLICATION_JSON, mockHttpOutputMessage);
        return mockHttpOutputMessage.getBodyAsString();
    }

}

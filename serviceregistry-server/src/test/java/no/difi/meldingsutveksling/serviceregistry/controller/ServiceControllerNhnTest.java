package no.difi.meldingsutveksling.serviceregistry.controller;


import com.fasterxml.jackson.databind.ObjectMapper;
import no.difi.meldingsutveksling.serviceregistry.SRRequestScope;
import no.difi.meldingsutveksling.serviceregistry.domain.CitizenInfo;
import no.difi.meldingsutveksling.serviceregistry.domain.DocumentType;
import no.difi.meldingsutveksling.serviceregistry.domain.Process;
import no.difi.meldingsutveksling.serviceregistry.domain.ProcessCategory;
import no.difi.meldingsutveksling.serviceregistry.domain.ServiceIdentifier;
import no.difi.meldingsutveksling.serviceregistry.exceptions.ServiceRegistryException;
import no.difi.meldingsutveksling.serviceregistry.record.HealthCareServiceRecord;
import no.difi.meldingsutveksling.serviceregistry.record.ServiceRecordService;
import no.difi.meldingsutveksling.serviceregistry.security.PayloadSigner;
import no.difi.meldingsutveksling.serviceregistry.service.AuthenticationService;
import no.difi.meldingsutveksling.serviceregistry.service.EntityService;
import no.difi.meldingsutveksling.serviceregistry.service.ProcessService;
import no.difi.meldingsutveksling.serviceregistry.service.healthcare.Patient;
import no.difi.meldingsutveksling.serviceregistry.service.healthcare.PatientNotRetrievedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.client.MockMvcWebTestClient;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;


@WebMvcTest(value = ServiceRecordController.class,properties = {"difi.move.healthcare.enabled=true"})
@ActiveProfiles("test")// Inline property
@WithMockUser
public class ServiceControllerNhnTest {

    @MockitoBean
    private AuthenticationService authenticationService;

    @MockitoBean
    private ServiceRecordService serviceRecordService;

    @MockitoBean
    private EntityService entityService;

    @MockitoBean
    private ProcessService processService;

    @MockitoBean
    private SRRequestScope requestScope;

    @MockitoBean
    private PayloadSigner payloadSigner;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;


    private WebTestClient webClient;

    private static String FNR = "21905297101";
    private static String HerId2 = "43434";
    private static final String ON_BEHALF_OF_ORGNUM = "345345435435";


    private static final String DPH_FASTLEGE = "urn:no:difi:profile:digitalpost:fastlege:ver1.0";
    private static final String DPH_NHN = "urn:no:difi:profile:digitalpost:helse:ver1.0";

    private static final String DPH_DIALOGMELDING = "urn:no:difi:digitalpost:json:schema::dialogmelding";


    private static final String identifierFastlegeEndpoint = "/identifier/" + FNR;
    private static final String identifierHerIDEndpoint = "/identifier/" + HerId2;

    private static final Process fastlegeProcess = new Process().setIdentifier(DPH_FASTLEGE).setCategory(ProcessCategory.DIALOGMELDING).setDocumentTypes(List.of(new DocumentType()
            .setIdentifier(DPH_DIALOGMELDING)));
    private static final Process nhnProcess = new Process().setIdentifier(DPH_NHN).setCategory(ProcessCategory.DIALOGMELDING).setDocumentTypes(List.of(new DocumentType()
            .setIdentifier(DPH_DIALOGMELDING)));


    @BeforeEach
    public void setup() {
        when(authenticationService.getAuthorizedClientIdentifier(any(), any())).thenReturn(ON_BEHALF_OF_ORGNUM);
        lenient().when(processService.findByIdentifier(DPH_FASTLEGE)).thenReturn(Optional.of(fastlegeProcess));
        lenient().when(processService.findByIdentifier(DPH_NHN)).thenReturn(Optional.of(nhnProcess));
        webClient = MockMvcWebTestClient.bindTo(mockMvc).codecs(t -> {
            t.defaultCodecs()
                    .jackson2JsonDecoder(new Jackson2JsonDecoder(objectMapper));
            t.defaultCodecs()
                    .jackson2JsonEncoder(new Jackson2JsonEncoder(objectMapper));
        }).build();
    }


    @Test
    public void whenIdentifierIsFnr_thenDphServiceRecord() throws Exception {
        when(entityService.getEntityInfo(FNR)).thenReturn(Optional.of(new CitizenInfo(FNR)));
        when(serviceRecordService.createHealthcareServiceRecords(argThat(t -> Objects.equals(t.getIdentifier(), FNR)))).thenReturn(List.of(new HealthCareServiceRecord(ServiceIdentifier.DPH, ON_BEHALF_OF_ORGNUM, fastlegeProcess, "dummyURl", "43234", HerId2, new Patient(FNR, "Petter", "", "Petterson"))));
        var response = webClient.get().uri(identifierFastlegeEndpoint + "/process/" + DPH_FASTLEGE).accept(MediaType.APPLICATION_JSON).exchange();

        response.expectStatus().isOk().expectBody().jsonPath("$.infoRecord.identifier").isEqualTo(FNR)
                .jsonPath("$.serviceRecords[0]").isNotEmpty()
                .jsonPath("$.serviceRecords[0].service.identifier").isEqualTo(ServiceIdentifier.DPH)
                .jsonPath("$.serviceRecords[0].process").isEqualTo(DPH_FASTLEGE)
                .jsonPath("$.serviceRecords[0].service").isNotEmpty()
                .jsonPath("$.serviceRecords[0].herIdLevel1").value(t -> Integer.parseInt((String) t))
                .jsonPath("$.serviceRecords[0].herIdLevel2").isEqualTo(HerId2)
                .jsonPath("$.serviceRecords[0].documentTypes").isNotEmpty()
                .jsonPath("$.serviceRecords[0].documentTypes[0]").isEqualTo(DPH_DIALOGMELDING);
    }


    @Test
    public void whenEntityServiceEmptyResult_then404() throws Exception {
        when(entityService.getEntityInfo(HerId2)).thenReturn(Optional.empty());
        webClient.get().uri(identifierHerIDEndpoint + "/process/" + DPH_FASTLEGE).accept(MediaType.APPLICATION_JSON).exchange().expectStatus().isNotFound();
    }


    @Test
    public void whenServiceRegistryException_then500() throws Exception {
        when(entityService.getEntityInfo(HerId2)).thenThrow(new ServiceRegistryException(HerId2));
        webClient.get().uri(identifierHerIDEndpoint + "/process/" + DPH_FASTLEGE).accept(MediaType.APPLICATION_JSON).exchange().expectStatus().is5xxServerError();
    }

    @Test
    public void whenPatientNotRetrievedException_then404() throws Exception {

        when(entityService.getEntityInfo(FNR)).thenReturn(Optional.of(new CitizenInfo(FNR)));
        lenient().when(processService.findByIdentifier(DPH_FASTLEGE)).thenReturn(Optional.of(fastlegeProcess));
        when(serviceRecordService.createHealthcareServiceRecords(argThat(t -> Objects.equals(t.getIdentifier(), FNR)))).thenThrow(new PatientNotRetrievedException());

        webClient.get().uri(identifierHerIDEndpoint + "/process/" + DPH_FASTLEGE).accept(MediaType.APPLICATION_JSON).exchange().expectStatus().is4xxClientError();

    }

}


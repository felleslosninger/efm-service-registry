package no.difi.meldingsutveksling.serviceregistry.servicerecord;

import no.difi.meldingsutveksling.serviceregistry.config.ServiceregistryProperties;
import no.difi.meldingsutveksling.serviceregistry.exceptions.EndpointUrlNotFound;
import no.difi.meldingsutveksling.serviceregistry.model.DocumentType;
import no.difi.meldingsutveksling.serviceregistry.model.Process;
import no.difi.meldingsutveksling.serviceregistry.model.ProcessCategory;
import no.difi.meldingsutveksling.serviceregistry.model.ServiceIdentifier;
import no.difi.meldingsutveksling.serviceregistry.service.ProcessService;
import no.difi.meldingsutveksling.serviceregistry.service.brreg.BrregService;
import no.difi.meldingsutveksling.serviceregistry.service.elma.ELMALookupService;
import no.difi.meldingsutveksling.serviceregistry.service.krr.KrrService;
import no.difi.meldingsutveksling.serviceregistry.service.virksert.VirkSertService;
import no.difi.meldingsutveksling.serviceregistry.svarut.SvarUtService;
import no.difi.move.common.oauth.KeystoreHelper;
import no.difi.vefa.peppol.common.model.*;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.ws.transport.http.HttpComponentsMessageSender;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ServiceRecordFactoryTest {

    @Autowired
    private ServiceRecordFactory factory;

    @MockBean
    private VirkSertService virkSertService;

    @MockBean
    private ELMALookupService lookupService;

    @MockBean
    private KrrService krrService;

    @MockBean
    private BrregService brregService;

    @MockBean
    private SvarUtService svarUtService;

    @MockBean
    private ProcessService processService;

    @MockBean
    private KeystoreHelper keystoreHelper;

    @MockBean
    private HttpComponentsMessageSender httpComponentsMessageSender;

    @MockBean
    private ServiceregistryProperties props;

    private static String ORGNR = "123456789";
    private static String ORGNR_FIKS = "987654321";
    private static String ARKIVMELDING_DOCTYPE = "urn:no:difi:arkivmelding:xsd::arkivmelding";
    private static String ARKIVMELDING_PROCESS_ADMIN = "urn:no:difi:profile:arkivmelding:administrasjon:ver1.0";
    private static String ARKIVMELDING_PROCESS_SKATT = "urn:no:difi:profile:arkivmelding:skatterOgAvgifter:ver1.0";

    @Before
    public void init() throws URISyntaxException, EndpointUrlNotFound, MalformedURLException {
        ServiceregistryProperties.Altinn altinn = new ServiceregistryProperties.Altinn();
        altinn.setEndpointURL(new URL("http://test"));
        altinn.setServiceCode("1234");
        altinn.setServiceEditionCode("123456");
        when(props.getDpo()).thenReturn(altinn);

        ServiceregistryProperties.PostVirksomhet dpv = new ServiceregistryProperties.PostVirksomhet();
        dpv.setEndpointURL(new URL("http://foo"));
        when(props.getDpv()).thenReturn(dpv);

        Endpoint ep = mock(Endpoint.class);
        when(ep.getAddress()).thenReturn(new URI(""));
        when(lookupService.lookup(any())).thenReturn(ep);

        when(svarUtService.hasSvarUtAdressering(ORGNR_FIKS)).thenReturn(Optional.of(3));

        DocumentType documentType = new DocumentType();
        documentType.setIdentifier(ARKIVMELDING_DOCTYPE);

        Process processAdmin = new Process();
        processAdmin.setIdentifier(ARKIVMELDING_PROCESS_ADMIN);
        processAdmin.setCategory(ProcessCategory.ARKIVMELDING);
        processAdmin.setServiceCode("4192");
        processAdmin.setServiceEditionCode("270815");

        Process processSkatt = new Process();
        processSkatt.setIdentifier(ARKIVMELDING_PROCESS_SKATT);
        processSkatt.setCategory(ProcessCategory.ARKIVMELDING);
        processSkatt.setServiceCode("4192");
        processSkatt.setServiceEditionCode("270815");

        processSkatt.setDocumentTypes(Lists.newArrayList(documentType));
        processAdmin.setDocumentTypes(Lists.newArrayList(documentType));
        documentType.setProcesses(Lists.newArrayList(processAdmin, processSkatt));

        when(processService.findAll()).thenReturn(Lists.newArrayList(processAdmin, processSkatt));
        when(processService.findAll(ProcessCategory.ARKIVMELDING)).thenReturn(Lists.newArrayList(processAdmin, processSkatt));

        ProcessMetadata<Endpoint> pmd = ProcessMetadata.of(ProcessIdentifier.of(ARKIVMELDING_PROCESS_ADMIN), Endpoint.of(null, null, null));
        ServiceMetadata smd = ServiceMetadata.of(ParticipantIdentifier.of("9908:" + ORGNR), DocumentTypeIdentifier.of(ARKIVMELDING_DOCTYPE), Arrays.asList(pmd));
        when(lookupService.lookup(Matchers.eq(ORGNR), any(List.class))).thenReturn(Lists.newArrayList(smd));
        when(lookupService.lookup(Matchers.eq(ORGNR_FIKS), any(List.class))).thenReturn(Lists.newArrayList());

    }

    @Test
    public void arkivmeldingDpoDpvServiceRecordTest() {
        List<ServiceRecord> arkivmeldingServiceRecords = factory.createArkivmeldingServiceRecords(ORGNR);
        assertEquals(2, arkivmeldingServiceRecords.size());

        ServiceRecord srAdmin = arkivmeldingServiceRecords.stream().filter(r -> ARKIVMELDING_PROCESS_ADMIN.equals(r.getProcess())).findFirst().orElseThrow(RuntimeException::new);
        assertEquals(ServiceIdentifier.DPO, srAdmin.getService().getIdentifier());
        ServiceRecord srSkatt = arkivmeldingServiceRecords.stream().filter(r -> ARKIVMELDING_PROCESS_SKATT.equals(r.getProcess())).findFirst().orElseThrow(RuntimeException::new);
        assertEquals(ServiceIdentifier.DPV, srSkatt.getService().getIdentifier());

    }

    @Test
    public void arkivmeldingDpfServiceRecordTest() throws MalformedURLException {
        ServiceregistryProperties.SvarUt svarUt = new ServiceregistryProperties.SvarUt();
        svarUt.setCertificate(new ByteArrayResource("cert1234".getBytes()));
        svarUt.setServiceRecordUrl(new URL("http://foo"));
        when(props.getSvarut()).thenReturn(svarUt);

        List<ServiceRecord> arkivmeldingServiceRecords = factory.createArkivmeldingServiceRecords(ORGNR_FIKS);
        assertEquals(2, arkivmeldingServiceRecords.stream().filter(r -> ServiceIdentifier.DPF == r.getService().getIdentifier()).count());
    }

    // TODO add tests for digitalpost and einnsyn
}

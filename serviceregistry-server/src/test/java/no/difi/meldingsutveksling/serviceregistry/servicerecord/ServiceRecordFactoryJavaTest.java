package no.difi.meldingsutveksling.serviceregistry.servicerecord;

import no.difi.meldingsutveksling.serviceregistry.CertificateNotFoundException;
import no.difi.meldingsutveksling.serviceregistry.exceptions.EndpointUrlNotFound;
import no.difi.meldingsutveksling.serviceregistry.service.brreg.BrregService;
import no.difi.meldingsutveksling.serviceregistry.service.elma.ELMALookupService;
import no.difi.meldingsutveksling.serviceregistry.service.krr.KrrService;
import no.difi.meldingsutveksling.serviceregistry.service.virksert.VirkSertService;
import no.difi.meldingsutveksling.serviceregistry.svarut.SvarUtService;
import no.difi.vefa.peppol.common.lang.PeppolException;
import no.difi.vefa.peppol.common.model.Endpoint;
import no.difi.virksert.client.lang.VirksertClientException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.HttpClientErrorException;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

import static no.difi.meldingsutveksling.serviceregistry.model.ServiceIdentifier.DPE_INNSYN;
import static no.difi.meldingsutveksling.serviceregistry.model.ServiceIdentifier.DPF;
import static no.difi.meldingsutveksling.serviceregistry.model.ServiceIdentifier.DPO;
import static org.assertj.core.util.Strings.isNullOrEmpty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ServiceRecordFactoryJavaTest {

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

    public static String ORGNR = "123456789";

    @Before
    public void init() throws URISyntaxException, EndpointUrlNotFound {
        Endpoint ep = mock(Endpoint.class);
        when(ep.getAddress()).thenReturn(new URI(""));
        when(lookupService.lookup(any())).thenReturn(ep);
    }

    @Test
    public void eduShouldReturnServiceRecord() throws VirksertClientException {
        when(virkSertService.getCertificate(any())).thenReturn("pem123");

        Optional<ServiceRecord> serviceRecord = factory.createEduServiceRecord(ORGNR);
        assertTrue(serviceRecord.isPresent());
        ServiceRecord record = serviceRecord.get();
        assertTrue(record instanceof EDUServiceRecord);
        assertEquals(ORGNR, record.getOrganisationNumber());
        assertTrue(!isNullOrEmpty(record.getPemCertificate()));
    }

    @Test
    public void eduShouldReturnEmptyRecord() throws EndpointUrlNotFound {
        when(lookupService.lookup(any())).thenThrow(new EndpointUrlNotFound("not found in ELMA", mock(PeppolException.class)));
        assertTrue(!factory.createEduServiceRecord(ORGNR).isPresent());
    }

    @Test
    public void eduShouldReturnErrorRecord() throws VirksertClientException {
        when(virkSertService.getCertificate(any())).thenThrow(new CertificateNotFoundException("", mock(VirksertClientException.class)));

        Optional<ServiceRecord> serviceRecord = factory.createEduServiceRecord(ORGNR);
        assertTrue(serviceRecord.isPresent());
        ServiceRecord record = serviceRecord.get();
        assertTrue(record instanceof ErrorServiceRecord);
        assertEquals(DPO, record.getServiceIdentifier());
    }

    @Test
    public void fiksShouldReturnServiceRecord() {
        when(svarUtService.hasSvarUtAdressering(ORGNR)).thenReturn(true);

        Optional<ServiceRecord> serviceRecord = factory.createFiksServiceRecord(ORGNR);
        assertTrue(serviceRecord.isPresent());
        ServiceRecord record = serviceRecord.get();
        assertTrue(record instanceof FiksServiceRecord);
        assertEquals(ORGNR, record.getOrganisationNumber());
        assertTrue(!isNullOrEmpty(record.getPemCertificate()));
    }

    @Test
    public void fiksShouldReturnEmptyRecord() {
        when(svarUtService.hasSvarUtAdressering(ORGNR)).thenReturn(false);
        assertTrue(!factory.createFiksServiceRecord(ORGNR).isPresent());
    }

    @Test
    public void fiksShouldReturnErrorRecord() {
        when(svarUtService.hasSvarUtAdressering(any())).thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));

        Optional<ServiceRecord> serviceRecord = factory.createFiksServiceRecord(ORGNR);
        assertTrue(serviceRecord.isPresent());
        ServiceRecord record = serviceRecord.get();
        assertTrue(record instanceof ErrorServiceRecord);
        assertEquals(DPF, record.getServiceIdentifier());
    }

    @Test
    public void dpeShouldReturnServiceRecord() throws VirksertClientException {
        when(lookupService.identifierHasInnsynskravCapability(any())).thenReturn(true);
        when(virkSertService.getCertificate(any())).thenReturn("pem123");

        Optional<ServiceRecord> serviceRecord = factory.createDpeInnsynServiceRecord(ORGNR);
        assertTrue(serviceRecord.isPresent());
        ServiceRecord record = serviceRecord.get();
        assertTrue(record instanceof DpeServiceRecord);
        assertEquals(ORGNR, record.getOrganisationNumber());
        assertTrue(!isNullOrEmpty(record.getPemCertificate()));
    }

    @Test
    public void dpeShouldReturnEmptyRecord() {
        when(lookupService.identifierHasInnsynskravCapability(any())).thenReturn(false);
        assertTrue(!factory.createDpeInnsynServiceRecord(ORGNR).isPresent());
    }

    @Test
    public void dpeShouldReturnErrorRecord() throws VirksertClientException {
        when(lookupService.identifierHasInnsynskravCapability(any())).thenReturn(true);
        when(virkSertService.getCertificate(any())).thenThrow(new CertificateNotFoundException("", mock(VirksertClientException.class)));

        Optional<ServiceRecord> serviceRecord = factory.createDpeInnsynServiceRecord(ORGNR);
        assertTrue(serviceRecord.isPresent());
        ServiceRecord record = serviceRecord.get();
        assertTrue(record instanceof ErrorServiceRecord);
        assertEquals(DPE_INNSYN, record.getServiceIdentifier());
    }
}

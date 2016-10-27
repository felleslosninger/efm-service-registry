package no.difi.meldingsutveksling.serviceregistry.servicerecord;

import java.net.MalformedURLException;
import java.net.URL;
import no.difi.meldingsutveksling.serviceregistry.config.ServiceregistryProperties;
import no.difi.meldingsutveksling.serviceregistry.service.elma.ELMALookupService;
import no.difi.meldingsutveksling.serviceregistry.service.ks.KSLookup;
import no.difi.meldingsutveksling.serviceregistry.service.ks.MockKSLookup;
import no.difi.vefa.peppol.common.lang.EndpointNotFoundException;
import no.difi.vefa.peppol.common.model.Endpoint;
import no.difi.vefa.peppol.lookup.api.LookupException;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import static org.mockito.Matchers.anyString;
import org.mockito.Mock;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ServiceRecordTests {

    private static final String ENDPOINT_URL = "http://altinn.no";
    private static final String ORGNR = "123456789";

    @Mock
    private ELMALookupService elmaLookupService;

    @Mock
    private ServiceregistryProperties properties;

    @InjectMocks
    private EDUServiceRecord eduServiceRecord;

    @InjectMocks
    private PostVirksomhetServiceRecord postVirksomhetServiceRecord;

    @Spy
    private KSLookup ksLookup = new MockKSLookup();

    @Before
    public void spyKsLookup() {
        when(ksLookup.mapOrganizationNumber(anyString())).thenCallRealMethod();
    }

    @After
    public void tearDown() {
        postVirksomhetServiceRecord = null;
        eduServiceRecord = null;
    }

    @Test
    public void testShouldGetEndPointFromEDUServiceRecord() throws LookupException, EndpointNotFoundException {
        // The EDURecord should lookup the Endpoint Using ELMA
        eduServiceRecord.setOrganisationNumber(ORGNR);
        when(elmaLookupService.lookup("9908:" + ORGNR)).thenReturn(new Endpoint(null, null, ENDPOINT_URL, null));
        assertEquals(ENDPOINT_URL, eduServiceRecord.getEndPointURL());
    }

    @Test
    public void testShouldgetEndPointForPostVirksomhetService() throws LookupException, MalformedURLException {
        // But ... The PostVirksomhetRecord should lookup the Endpoint from configuration file
        ServiceregistryProperties.PostVirksomhet mock = mock(ServiceregistryProperties.PostVirksomhet.class);
        when(properties.getDpv()).thenReturn(mock);
        when(mock.getEndpointURL()).thenReturn(new URL(ENDPOINT_URL));
        assertEquals(ENDPOINT_URL, postVirksomhetServiceRecord.getEndPointURL());
    }

    @Test
    public void testShouldSwapCertificateAndOrgNumberForKSManagedEDU() {
        eduServiceRecord.setOrganisationNumber(MockKSLookup.KS_MANAGED_1);
        assertEquals(MockKSLookup.KS_ORGNR, eduServiceRecord.getOrganisationNumber());
    }
}

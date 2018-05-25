package no.difi.meldingsutveksling.serviceregistry.service.brreg;

import no.difi.meldingsutveksling.serviceregistry.client.brreg.BrregClientImpl;
import no.difi.meldingsutveksling.serviceregistry.model.BrregEnhet;
import no.difi.meldingsutveksling.serviceregistry.model.OrganizationInfo;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

public class BrregServiceTest {
    private BrregService brregService;
    private OrganizationInfo difi;

    @Before
    public void setup() {
        String orgNavn = "DIREKTORATET FOR FORVALTNING OG IKT";
        String orgNr = "991825827";
        final String organisasjonsform = "ORGL";

        difi = new OrganizationInfo.Builder().withOrganizationType(organisasjonsform).withOrganizationName
                (orgNavn).withOrganizationNumber(orgNr).build();

        BrregClientImpl brregClientMock = setupMock(orgNavn, orgNr, organisasjonsform);
        DatahotellClient datahotellMock = mock(DatahotellClient.class);
        brregService = new BrregService(brregClientMock, datahotellMock);
    }

    @Test
    public void brregHasOrganizationInfo() throws BrregNotFoundException {
        assertEquals(difi, brregService.getOrganizationInfo(difi.getIdentifier()).get());
    }

    private BrregClientImpl setupMock(String orgNavn, String orgNr, String organisasjonsform) {
        BrregEnhet enhet = new BrregEnhet();
        enhet.setOrganisasjonsnummer(orgNr);
        enhet.setNavn(orgNavn);
        enhet.setOrganisasjonsform(organisasjonsform);

        BrregClientImpl brregClientMock = mock(BrregClientImpl.class);
        Mockito.when(brregClientMock.getBrregEnhetByOrgnr(Mockito.anyString())).thenReturn(Optional.empty());
        Mockito.when(brregClientMock.getBrregEnhetByOrgnr(String.valueOf(orgNr))).thenReturn(Optional.of(enhet));
        return brregClientMock;
    }
}

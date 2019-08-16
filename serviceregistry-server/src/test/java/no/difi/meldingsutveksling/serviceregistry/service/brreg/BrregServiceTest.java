package no.difi.meldingsutveksling.serviceregistry.service.brreg;

import no.difi.meldingsutveksling.serviceregistry.client.brreg.BrregClientImpl;
import no.difi.meldingsutveksling.serviceregistry.model.BrregEnhet;
import no.difi.meldingsutveksling.serviceregistry.model.BrregPostadresse;
import no.difi.meldingsutveksling.serviceregistry.model.EntityInfo;
import no.difi.meldingsutveksling.serviceregistry.model.OrganizationInfo;
import no.difi.meldingsutveksling.serviceregistry.util.SRRequestScope;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

public class BrregServiceTest {
    private BrregService brregService;
    private OrganizationInfo difi;
    private OrganizationInfo donna;
    BrregClientImpl brregClientMock;

    @Before
    public void setup() {
        brregClientMock = mock(BrregClientImpl.class);
    }

    @Test
    public void brregHasOrganizationInfo() throws BrregNotFoundException {
        String orgNavn = "DIREKTORATET FOR FORVALTNING OG IKT";
        String orgNr = "991825827";
        final String organisasjonsform = "ORGL";
        BrregPostadresse postAddr = new BrregPostadresse("Skrivarvegen 2", "6863", "Hermansverk", "NO");

        difi = new OrganizationInfo.Builder()
                .withOrganizationType(organisasjonsform)
                .withOrganizationName(orgNavn)
                .withOrganizationNumber(orgNr)
                .withPostadresse(postAddr)
                .build();

        BrregEnhet enhet = new BrregEnhet();
        enhet.setOrganisasjonsnummer(orgNr);
        enhet.setNavn(orgNavn);
        enhet.setOrganisasjonsform(organisasjonsform);
        enhet.setPostadresse(postAddr);

        Mockito.when(brregClientMock.getBrregEnhetByOrgnr(Mockito.anyString())).thenReturn(Optional.empty());
        Mockito.when(brregClientMock.getBrregEnhetByOrgnr(orgNr)).thenReturn(Optional.of(enhet));
        DatahotellClient datahotellMock = mock(DatahotellClient.class);
        brregService = new BrregService(brregClientMock, datahotellMock);

        OrganizationInfo actual = (OrganizationInfo) brregService.getOrganizationInfo(difi.getIdentifier()).get();
        assertEquals(difi, actual);
    }

    @Test
    public void shouldReturnBusinessAddressIfOrgHasNoPostAddress() throws BrregNotFoundException {
        String orgNavn2 = "DØNNA KOMMUNE";
        String orgNr2 = "945114878";
        final String organisasjonsform2 = "KOMM";
        BrregPostadresse forretningsAddr2 = new BrregPostadresse("Skrivarvegen 42", "6863", "Hermansverk", "NO");

        donna = new OrganizationInfo.Builder()
                .withOrganizationType(organisasjonsform2)
                .withOrganizationName(orgNavn2)
                .withOrganizationNumber(orgNr2)
                .withPostadresse(forretningsAddr2)
                .build();

        BrregEnhet enhet2 = new BrregEnhet();
        enhet2.setOrganisasjonsnummer(orgNr2);
        enhet2.setNavn(orgNavn2);
        enhet2.setOrganisasjonsform(organisasjonsform2);
        enhet2.setForretningsadresse(forretningsAddr2);

        Mockito.when(brregClientMock.getBrregEnhetByOrgnr(Mockito.anyString())).thenReturn(Optional.empty());
        Mockito.when(brregClientMock.getBrregEnhetByOrgnr(orgNr2)).thenReturn(Optional.of(enhet2));
        DatahotellClient datahotellMock = mock(DatahotellClient.class);
        brregService = new BrregService(brregClientMock, datahotellMock);

        OrganizationInfo actual = (OrganizationInfo) brregService.getOrganizationInfo(donna.getIdentifier()).get();
        assertEquals(donna.getPostadresse(), actual.getPostadresse());
    }

}

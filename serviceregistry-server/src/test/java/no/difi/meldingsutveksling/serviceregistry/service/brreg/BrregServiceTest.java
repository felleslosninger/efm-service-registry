package no.difi.meldingsutveksling.serviceregistry.service.brreg;

import no.difi.meldingsutveksling.serviceregistry.client.brreg.BrregClientImpl;
import no.difi.meldingsutveksling.serviceregistry.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

public class BrregServiceTest {
    private BrregService brregService;
    BrregClientImpl brregClientMock;

    @BeforeEach
    public void setup() {
        brregClientMock = mock(BrregClientImpl.class);
    }

    @Test
    public void brregHasOrganizationInfo() throws BrregNotFoundException {
        String orgNavn = "DIREKTORATET FOR FORVALTNING OG IKT";
        String orgNr = "991825827";
        final String orgKode = "ORGL";
        BrregOrganisasjonsform organisasjonsform = new BrregOrganisasjonsform(orgKode);
        BrregPostadresse brregPostAddr = new BrregPostadresse(Collections.singletonList("Skrivarvegen 42"), "6863", "Hermansverk", "NO");
        Postadresse postAddr = Postadresse.of(brregPostAddr);

        OrganizationInfo difi = new OrganizationInfo.Builder()
                .withOrganizationType(orgKode)
                .withOrganizationName(orgNavn)
                .withOrganizationNumber(orgNr)
                .withPostadresse(postAddr)
                .build();

        BrregEnhet enhet = new BrregEnhet();
        enhet.setOrganisasjonsnummer(orgNr);
        enhet.setNavn(orgNavn);
        enhet.setOrganisasjonsform(organisasjonsform);
        enhet.setPostadresse(brregPostAddr);

        Mockito.when(brregClientMock.getBrregEnhetByOrgnr(Mockito.anyString())).thenReturn(Optional.empty());
        Mockito.when(brregClientMock.getBrregEnhetByOrgnr(orgNr)).thenReturn(Optional.of(enhet));
        brregService = new BrregService(brregClientMock);

        OrganizationInfo actual = (OrganizationInfo) brregService.getOrganizationInfo(difi.getIdentifier()).get();
        assertEquals(difi, actual);
    }

    @Test
    public void shouldReturnBusinessAddressIfOrgHasNoPostAddress() throws BrregNotFoundException {
        String orgNavn2 = "DØNNA KOMMUNE";
        String orgNr2 = "945114878";
        final String orgKode2 = "KOMM";
        BrregOrganisasjonsform organisasjonsform2 = new BrregOrganisasjonsform(orgKode2);
        BrregPostadresse brregForretningsAddr2 = new BrregPostadresse(Collections.singletonList("Skrivarvegen 42"), "6863", "Hermansverk", "NO");
        Postadresse forretningsAddr2 = Postadresse.of(brregForretningsAddr2);

        OrganizationInfo donna = new OrganizationInfo.Builder()
                .withOrganizationType(orgKode2)
                .withOrganizationName(orgNavn2)
                .withOrganizationNumber(orgNr2)
                .withPostadresse(forretningsAddr2)
                .build();

        BrregEnhet enhet2 = new BrregEnhet();
        enhet2.setOrganisasjonsnummer(orgNr2);
        enhet2.setNavn(orgNavn2);
        enhet2.setOrganisasjonsform(organisasjonsform2);
        enhet2.setForretningsadresse(brregForretningsAddr2);

        Mockito.when(brregClientMock.getBrregEnhetByOrgnr(Mockito.anyString())).thenReturn(Optional.empty());
        Mockito.when(brregClientMock.getBrregEnhetByOrgnr(orgNr2)).thenReturn(Optional.of(enhet2));
        brregService = new BrregService(brregClientMock);

        OrganizationInfo actual = (OrganizationInfo) brregService.getOrganizationInfo(donna.getIdentifier()).get();
        assertEquals(donna.getPostadresse(), actual.getPostadresse());
    }

}

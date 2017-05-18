package no.difi.meldingsutveksling.serviceregistry.businesslogic;

import no.difi.meldingsutveksling.serviceregistry.model.OrganizationInfo;
import no.difi.meldingsutveksling.serviceregistry.model.OrganizationType;
import no.difi.meldingsutveksling.serviceregistry.model.OrganizationTypes;
import org.junit.Test;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.*;

public class ServiceRecordPredicatesTest {

    @Test
    public void identifierContainsLettersShouldNotBeCitizen() {
        final String identifier = "as123456789";

        final boolean result = ServiceRecordPredicates.isCitizen().test(identifier);

        assertThat("Identifier is not a valid citizen identifier", result, equalTo(false));
    }

    @Test
    public void identifierShouldBeCitizen() {
        final String identifier = "06068700602";

        final boolean result = ServiceRecordPredicates.isCitizen().test(identifier);

        assertThat("Identifier should belong to a citizen", result, equalTo(true));
    }

    @Test
    public void identierBelongingToOrganizationShouldNotBeCitizen() {
        final String identifier = "991825827";

        final boolean result = ServiceRecordPredicates.isCitizen().test(identifier);

        assertThat("Identifier should belong to a citizen", result, equalTo(false));
    }

    @Test
    public void messagesToCitizenUsesSikkerDigitalPost() {
        final OrganizationInfo citizen = new OrganizationInfo.Builder().withOrganizationNumber("06068700602").build();

        final boolean result = ServiceRecordPredicates.shouldCreateServiceRecordForCititzen().test(citizen);

        assertThat("Citizen should use sikker digital post", result, equalTo(true));
    }

    @Test
    public void messagesToPrivateOrganizationsUsesPostTilVirksomhet() {
        final OrganizationInfo privateOrganization = privateOrganization();

        final boolean result = ServiceRecordPredicates.usesPostTilVirksomhet().test(privateOrganization);

        assertThat("Private organization should use post til virksomheter", result, equalTo(true));
    }

    @Test
    public void messagesToPublicOrganizationsUsesFormidlingstjenesten() {
        final OrganizationInfo orgl = publicOrganization();

        final boolean result = ServiceRecordPredicates.usesFormidlingstjenesten().test(orgl);

        assertThat("Public organization should use formidlingstjenesten", result, equalTo(true));
    }

    private OrganizationInfo privateOrganization() {
        final OrganizationType privateType = new OrganizationTypes().privateOrganization().stream().findFirst().orElseThrow(() -> new RuntimeException("Could not find a private organization"));
        return new OrganizationInfo.Builder().withOrganizationType(privateType).build();
    }

    private OrganizationInfo publicOrganization() {
        final OrganizationType publicType = new OrganizationTypes().publicOrganization().stream().findFirst().orElseThrow(() -> new RuntimeException("Could not find a public organization type"));
        return new OrganizationInfo.Builder().withOrganizationType(publicType).build();
    }
}
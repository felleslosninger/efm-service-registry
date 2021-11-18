package no.difi.meldingsutveksling.serviceregistry.businesslogic;

import no.difi.meldingsutveksling.serviceregistry.domain.OrganizationInfo;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

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

        final boolean result = ServiceRecordPredicates.shouldCreateServiceRecordForCitizen().test(citizen);

        assertThat("Citizen should use sikker digital post", result, equalTo(true));
    }

}
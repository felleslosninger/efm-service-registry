package no.difi.meldingsutveksling.serviceregistry.businesslogic;

import no.difi.meldingsutveksling.serviceregistry.model.OrganizationInfo;
import no.difi.meldingsutveksling.serviceregistry.model.OrganizationType;
import no.difi.meldingsutveksling.serviceregistry.model.OrganizationTypes;

import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Contains predicates used to determine the type of service record to create/use to send messages
 */
public class ServiceRecordPredicates {

    private static final Predicate<String> exactly_11_numbers = Pattern.compile(String.format("\\d{%d}", 11)).asPredicate();

    private ServiceRecordPredicates() {
    }

    public static Predicate<OrganizationInfo> usesPostTilVirksomhet() {
        Set<OrganizationType> privateOrganizationTypes = new OrganizationTypes().privateOrganization();
        return o -> privateOrganizationTypes.contains(o.getOrganizationType());
    }

    public static Predicate<OrganizationInfo> usesFormidlingstjenesten() {
        Set<OrganizationType> publicOrganizationTypes = new OrganizationTypes().publicOrganization();
        return o -> publicOrganizationTypes.contains(o.getOrganizationType());
    }

    public static Predicate<OrganizationInfo> usesSikkerDigitalPost() {
        return o -> isCitizen().test(o.getOrganisationNumber());
    }

    /**
     * Certain services are not available to citizens for instance BRREG enhetsregister
     *
     * @return predicate to test whether a String has the correct format of a fodselsnummer
     */
    public static Predicate<String> isCitizen() {
        // This is actually an oversimplification based on minimum government requirements for a fodselsnummer
        // which is sufficient for the time being

        return exactly_11_numbers;
    }
}

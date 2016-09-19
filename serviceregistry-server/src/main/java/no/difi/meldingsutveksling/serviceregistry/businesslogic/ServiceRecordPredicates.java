package no.difi.meldingsutveksling.serviceregistry.businesslogic;

import no.difi.meldingsutveksling.serviceregistry.model.OrganizationInfo;
import no.difi.meldingsutveksling.serviceregistry.model.OrganizationType;
import no.difi.meldingsutveksling.serviceregistry.model.OrganizationTypes;

import java.util.Set;
import java.util.function.Predicate;

/**
 * Contains predicates used to determine the type of service record to create/use to send messages
 */
public class ServiceRecordPredicates {

    private ServiceRecordPredicates() {
    }

    private static final int LENGTH_OF_FODSELSNUMMER = 11;

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
        return s -> s.matches(String.format("\\d{%d}", LENGTH_OF_FODSELSNUMMER));
    }
}

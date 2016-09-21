package no.difi.meldingsutveksling.serviceregistry.businesslogic;

import no.difi.meldingsutveksling.serviceregistry.model.*;

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

    /**
     * Predicate to test whether Post til virksomheter is a valid service for sending messages
     * @return predicate that evaluates true if able to use post til virksomhet
     */
    public static Predicate<EntityInfo> usesPostTilVirksomhet() {
        Set<? extends EntityType> privateOrganizationTypes = new OrganizationTypes().privateOrganization();
        return o -> privateOrganizationTypes.contains(o.getEntityType());
    }

    /**
     * Predicate to test whether Formidlingstjenesten is a valid service for sending messages
     * @return predicate that evaluates true if able to use formidlingstjenesten
     */
    public static Predicate<EntityInfo> usesFormidlingstjenesten() {
        Set<? extends EntityType> publicOrganizationTypes = new OrganizationTypes().publicOrganization();
        return o -> publicOrganizationTypes.contains(o.getEntityType());
    }

    /**
     * Predicate to test whether Sikker digital post is a valid service for sending messages
     * @return predicate that evaluates true if able to use sikker digital post
     */
    public static Predicate<EntityInfo> usesSikkerDigitalPost() {
        return o -> isCitizen().test(o.getIdentifier());
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

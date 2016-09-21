package no.difi.meldingsutveksling.serviceregistry.model;

import com.google.common.base.Objects;

import java.io.Serializable;

/**
 * Represents an organization according to BRREG
 *
 * See http://hotell.difi.no/?dataset=brreg/organisasjonsform
 *
 */
public class OrganizationType implements Serializable, EntityType {
    private String name;
    private String acronym;

    /**
     * Constructs new instance
     * @param name for instance Organisasjonsledd
     * @param acronym for instance ORGL
     */
    OrganizationType(String name, String acronym) {
        this.name = name;
        this.acronym = acronym;
    }

    public String getName() {
        return name;
    }

    public String getAcronym() {
        return acronym;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrganizationType that = (OrganizationType) o;
        return Objects.equal(acronym, that.acronym);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(acronym);
    }

    public static OrganizationType from(String organisasjonsform) {
        return OrganizationTypes.all.get(organisasjonsform);
    }

}

package no.difi.meldingsutveksling.serviceregistry.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

/**
 * Contains information relevant for an organization
 */
@Data
public class OrganizationInfo implements EntityInfo {

    private String identifier;
    private String organizationName;
    @JsonIgnore
    private OrganizationType organizationType;
    private BrregPostadresse postadresse;

    // Needed by the JSON marshaller?
    public OrganizationInfo() {
    }

    /**
     * @param organisationNumber of the recipient organization
     * @param organizationName as name implies
     * @param organizationType or organization form as defined in BRREG
     */
    public OrganizationInfo(String organisationNumber, String organizationName, BrregPostadresse postadresse,
                            OrganizationType organizationType) {
        this.identifier = organisationNumber;
        this.organizationName = organizationName;
        this.organizationType = organizationType;
        this.postadresse = postadresse;
    }

    @Override
    public EntityType getEntityType() {
        return organizationType;
    }

    @Override
    public String getIdentifier() {
        return identifier;
    }

    public static class Builder {
        private OrganizationInfo organizationInfo = new OrganizationInfo();

        public Builder() {}

        /**
         * @return a new instance of OrganizationInfo
         */
        public OrganizationInfo build() {
            return organizationInfo;
        }

        public Builder withOrganizationNumber(String organisationNumber) {
            organizationInfo.identifier = organisationNumber;
            return this;
        }

        public Builder withOrganizationName(String organizationName) {
            organizationInfo.organizationName = organizationName;
            return this;
        }

        public Builder withOrganizationType(OrganizationType organizationType) {
            organizationInfo.organizationType = organizationType;
            return this;
        }

        public Builder withPostadresse(BrregPostadresse postadresse) {
            organizationInfo.postadresse = postadresse;
            return this;
        }
    }

}
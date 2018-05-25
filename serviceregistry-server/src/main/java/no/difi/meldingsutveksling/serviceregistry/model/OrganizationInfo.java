package no.difi.meldingsutveksling.serviceregistry.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import no.difi.meldingsutveksling.serviceregistry.model.datahotell.DatahotellEntry;

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

    public static OrganizationInfo of(BrregEnhet brregEnhet) {
        return new OrganizationInfo(brregEnhet.getOrganisasjonsnummer(),
                brregEnhet.getNavn(),
                brregEnhet.getPostadresse(),
                new OrganizationType(brregEnhet.getOrganisasjonsform()));
    }

    public static OrganizationInfo of(DatahotellEntry enhet) {
        BrregPostadresse postadresse = new BrregPostadresse(enhet.getPostadresse(),
                enhet.getPpostnr(),
                enhet.getPpoststed(),
                enhet.getPpostland());
        return new OrganizationInfo(enhet.getOrgnr(),
                enhet.getNavn(),
                postadresse,
                new OrganizationType(enhet.getOrganisasjonsform()));
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

        public Builder withOrganizationType(String organizationType) {
            organizationInfo.organizationType = new OrganizationType(organizationType);
            return this;
        }

        public Builder withPostadresse(BrregPostadresse postadresse) {
            organizationInfo.postadresse = postadresse;
            return this;
        }
    }

}
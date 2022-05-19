package no.difi.meldingsutveksling.serviceregistry.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import no.difi.meldingsutveksling.domain.ICD;
import no.difi.meldingsutveksling.domain.Iso6523;

import java.util.Collections;

import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * Contains information relevant for an organization
 */
@Data
public class OrganizationInfo implements EntityInfo {

    private String identifier;
    private String organizationName;
    @JsonIgnore
    private OrganizationType organizationType;
    private Postadresse postadresse;

    public OrganizationInfo() {
    }

    public OrganizationInfo(String orgnr, OrganizationType type) {
        this.identifier = orgnr;
        this.organizationType = type;
    }

    /**
     * @param organisationNumber of the recipient organization
     * @param organizationName   as name implies
     * @param organizationType   or organization form as defined in BRREG
     */
    public OrganizationInfo(String organisationNumber, String organizationName, Postadresse postadresse,
                            OrganizationType organizationType) {
        this.identifier = organisationNumber;
        this.organizationName = organizationName;
        this.organizationType = organizationType;
        this.postadresse = postadresse;
    }

    public static OrganizationInfo of(BrregEnhet brregEnhet, boolean usePlainFormat) {
        return new OrganizationInfo(usePlainFormat ? brregEnhet.getOrganisasjonsnummer() : Iso6523.of(ICD.NO_ORG, brregEnhet.getOrganisasjonsnummer()).getIdentifier(),
                brregEnhet.getNavn(),
                Postadresse.of(brregEnhet.getPostadresse() != null ?
                        brregEnhet.getPostadresse() : brregEnhet.getForretningsadresse()),
                new OrganizationType(brregEnhet.getOrganisasjonsform().getKode()));
    }

    public static OrganizationInfo of(DatahotellEntry enhet, boolean usePlainFormat) {
        BrregPostadresse postadresse;
        if (isNullOrEmpty(enhet.getPostadresse())
                || isNullOrEmpty(enhet.getPpostnr())
                || isNullOrEmpty(enhet.getPpoststed())
                || isNullOrEmpty(enhet.getPpostland())) {
            postadresse = new BrregPostadresse(Collections.singletonList(enhet.getForretningsadr()),
                    enhet.getForradrpostnr(),
                    enhet.getForradrpoststed(),
                    enhet.getForradrland());
        } else {
            postadresse = new BrregPostadresse(Collections.singletonList(enhet.getPostadresse()),
                    enhet.getPpostnr(),
                    enhet.getPpoststed(),
                    enhet.getPpostland());
        }
        return new OrganizationInfo(usePlainFormat ? enhet.getOrgnr() : Iso6523.of(ICD.NO_ORG, enhet.getOrgnr()).getIdentifier(),
                enhet.getNavn(),
                Postadresse.of(postadresse),
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

        public Builder() {
        }

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

        public Builder withPostadresse(Postadresse postadresse) {
            organizationInfo.postadresse = postadresse;
            return this;
        }
    }

}
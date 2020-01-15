package no.difi.meldingsutveksling.serviceregistry.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import no.difi.meldingsutveksling.serviceregistry.model.datahotell.DatahotellEntry;

import java.util.ArrayList;
import java.util.List;

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

    /**
     * @param organisationNumber of the recipient organization
     * @param organizationName as name implies
     * @param organizationType or organization form as defined in BRREG
     */
    public OrganizationInfo(String organisationNumber, String organizationName, Postadresse postadresse,
                            OrganizationType organizationType) {
        this.identifier = organisationNumber;
        this.organizationName = organizationName;
        this.organizationType = organizationType;
        this.postadresse = postadresse;
    }

    public static OrganizationInfo of(BrregEnhet brregEnhet) {
        if (brregEnhet.getPostadresse() == null) {
            return new OrganizationInfo(brregEnhet.getOrganisasjonsnummer(),
                    brregEnhet.getNavn(),
                    Postadresse.of(brregEnhet.getForretningsadresse()),
                    new OrganizationType(brregEnhet.getOrganisasjonsform().getKode()));
        }
        return new OrganizationInfo(brregEnhet.getOrganisasjonsnummer(),
                brregEnhet.getNavn(),
                Postadresse.of(brregEnhet.getPostadresse()),
                new OrganizationType(brregEnhet.getOrganisasjonsform().getKode()));
    }

    public static OrganizationInfo of(DatahotellEntry enhet) {
        BrregPostadresse postadresse;
        if (isNullOrEmpty(enhet.getPostadresse())
                || isNullOrEmpty(enhet.getPpostnr())
                || isNullOrEmpty(enhet.getPpoststed())
                || isNullOrEmpty(enhet.getPpostland())) {
            postadresse = new BrregPostadresse(getAdresseliste(enhet.getForretningsadr()),
                    enhet.getForradrpostnr(),
                    enhet.getForradrpoststed(),
                    enhet.getForradrland());
        } else {
            postadresse = new BrregPostadresse(getAdresseliste(enhet.getPostadresse()),
                    enhet.getPpostnr(),
                    enhet.getPpoststed(),
                    enhet.getPpostland());
        }
        return new OrganizationInfo(enhet.getOrgnr(),
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

        public Builder withPostadresse(Postadresse postadresse) {
            organizationInfo.postadresse = postadresse;
            return this;
        }
    }

    private static List<String> getAdresseliste(String adresse) {
        List<String> adresseListe = new ArrayList<>();
        adresseListe.add(adresse);
        return adresseListe;
    }

}
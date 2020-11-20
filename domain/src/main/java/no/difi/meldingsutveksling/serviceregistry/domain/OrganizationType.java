package no.difi.meldingsutveksling.serviceregistry.domain;

import lombok.Data;

import java.io.Serializable;

/**
 * Represents an organization according to BRREG
 *
 * See http://hotell.difi.no/?dataset=brreg/organisasjonsform
 *
 */
@Data
public class OrganizationType implements Serializable, EntityType {
    private String name;

    /**
     * Constructs new instance
     * @param name for instance Organisasjonsledd
     */
    public OrganizationType(String name) {
        this.name = name;
    }

}

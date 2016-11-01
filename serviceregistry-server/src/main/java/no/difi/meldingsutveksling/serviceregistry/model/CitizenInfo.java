package no.difi.meldingsutveksling.serviceregistry.model;

import java.io.Serializable;

public class CitizenInfo implements Serializable, EntityInfo {
    private final String identifier;

    public CitizenInfo(String identifier) {
        this.identifier = identifier;
    }

    @Override
    public EntityType getEntityType() {
        return new CitizenType("citizen");
    }

    @Override
    public String getIdentifier() {
        return identifier;
    }
}

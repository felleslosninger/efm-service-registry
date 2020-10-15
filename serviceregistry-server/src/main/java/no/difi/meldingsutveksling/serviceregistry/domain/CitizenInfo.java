package no.difi.meldingsutveksling.serviceregistry.domain;

public class CitizenInfo implements EntityInfo {
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

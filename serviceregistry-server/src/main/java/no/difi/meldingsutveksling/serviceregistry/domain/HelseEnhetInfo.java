package no.difi.meldingsutveksling.serviceregistry.domain;

public class HelseEnhetInfo implements EntityInfo {
    private String identifier;
    public static String HELSE_ENHET_ENTITY_TYPE = "HerID";

    public HelseEnhetInfo(String identifier) {
        this.identifier = identifier;
    }

    @Override
    public EntityType getEntityType() {
        return () -> HELSE_ENHET_ENTITY_TYPE;
    }

    @Override
    public String getIdentifier() {
        return this.identifier;
    }
}

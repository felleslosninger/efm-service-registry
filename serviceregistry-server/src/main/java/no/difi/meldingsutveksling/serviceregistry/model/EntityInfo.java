package no.difi.meldingsutveksling.serviceregistry.model;

/**
 * Information regarding the entity
 */
public interface EntityInfo {

    void setPrimaryServiceIdentifier(ServiceIdentifier serviceIdentifier);

    EntityType getEntityType();

    String getIdentifier();
}

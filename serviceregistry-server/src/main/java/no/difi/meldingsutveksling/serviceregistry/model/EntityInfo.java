package no.difi.meldingsutveksling.serviceregistry.model;

/**
 * Information regarding the entity
 */
public interface EntityInfo {

    EntityType getEntityType();

    String getIdentifier();
}

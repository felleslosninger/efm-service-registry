package no.difi.meldingsutveksling.serviceregistry.domain;

/**
 * Information regarding the entity
 */
public interface EntityInfo {

    EntityType getEntityType();

    String getIdentifier();
}

package no.difi.meldingsutveksling.serviceregistry.domain;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class FiksIoInfo implements EntityInfo {
    private final String identifier;

    @Override
    public EntityType getEntityType() {
        return new OrganizationType("FIKSIO");
    }

    @Override
    public String getIdentifier() {
        return identifier;
    }
}

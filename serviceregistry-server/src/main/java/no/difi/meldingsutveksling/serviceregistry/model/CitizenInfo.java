package no.difi.meldingsutveksling.serviceregistry.model;

import no.difi.meldingsutveksling.ptp.KontaktInfo;

import java.io.Serializable;

public class CitizenInfo implements Serializable, EntityInfo {
    private final String identifier;
    private ServiceIdentifier primaryServiceIdentifier;

    public CitizenInfo(String identifier) {
        this.identifier = identifier;
    }

    @Override
    public void setPrimaryServiceIdentifier(ServiceIdentifier serviceIdentifier) {
        this.primaryServiceIdentifier = serviceIdentifier;
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

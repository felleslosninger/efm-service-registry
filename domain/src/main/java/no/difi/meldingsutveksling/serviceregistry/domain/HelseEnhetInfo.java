package no.difi.meldingsutveksling.serviceregistry.domain;

import lombok.Value;

@Value
public class HelseEnhetInfo implements EntityInfo {

    String identifier;
    Integer parentHerId;
    String parentOrganizationName;
    Integer herId;
    String organizationName;
    String organizationNumber;
    String patient;

    @Override
    public EntityType getEntityType() {
        return () -> "NorskHelsenett";
    }

    @Override
    public String getIdentifier() {
        return identifier;
    }
}

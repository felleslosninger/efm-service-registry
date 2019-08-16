package no.difi.meldingsutveksling.serviceregistry.model;

import java.io.Serializable;

public class CitizenType implements Serializable, EntityType {
    public final String name;

    public CitizenType(String name) {
        this.name = name;
    }
}

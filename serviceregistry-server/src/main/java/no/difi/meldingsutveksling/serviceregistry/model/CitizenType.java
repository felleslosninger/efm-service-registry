package no.difi.meldingsutveksling.serviceregistry.model;

import java.io.Serializable;

public class CitizenType implements Serializable, EntityType {
    final public String name;

    public CitizenType(String name) {
        this.name = name;
    }


}

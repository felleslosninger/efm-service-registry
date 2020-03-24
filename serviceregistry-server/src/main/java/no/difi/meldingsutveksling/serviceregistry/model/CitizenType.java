package no.difi.meldingsutveksling.serviceregistry.model;

import lombok.Data;

import java.io.Serializable;

@Data
public class CitizenType implements Serializable, EntityType {
    public final String name;

    public CitizenType(String name) {
        this.name = name;
    }

}

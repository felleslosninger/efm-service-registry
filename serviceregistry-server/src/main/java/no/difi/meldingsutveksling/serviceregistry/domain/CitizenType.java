package no.difi.meldingsutveksling.serviceregistry.domain;

import lombok.Data;

import java.io.Serializable;

@Data
public class CitizenType implements Serializable, EntityType {
    public final String name;

    public CitizenType(String name) {
        this.name = name;
    }

}

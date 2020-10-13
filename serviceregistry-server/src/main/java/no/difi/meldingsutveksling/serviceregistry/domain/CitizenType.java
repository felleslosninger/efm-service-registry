package no.difi.meldingsutveksling.serviceregistry.domain;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.io.Serializable;

@Data
@RequiredArgsConstructor
public class CitizenType implements Serializable, EntityType {

    private final String name;

}

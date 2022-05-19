package no.difi.meldingsutveksling.serviceregistry.krr;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import no.difi.meldingsutveksling.domain.PersonIdentifier;

import java.util.ArrayList;
import java.util.List;

@Data
public class PersonRequest {

    @JsonProperty(value = "personidentifikatorer")
    private List<String> personIdentifiers = new ArrayList<>();

    public static PersonRequest of(PersonIdentifier identifier) {
        PersonRequest personRequest = new PersonRequest();
        personRequest.setPersonIdentifiers(List.of(identifier.getIdentifier()));
        return personRequest;
    }
}

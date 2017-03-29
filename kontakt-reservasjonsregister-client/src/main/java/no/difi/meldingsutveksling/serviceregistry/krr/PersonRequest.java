package no.difi.meldingsutveksling.serviceregistry.krr;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Data
public class PersonRequest {

    @JsonProperty(value = "personidentifikatorer")
    private List<String> personIdentifiers = new ArrayList<>();

    public static PersonRequest of(String identifier) {
        PersonRequest personRequest = new PersonRequest();
        personRequest.setPersonIdentifiers(Arrays.asList(identifier));
        return personRequest;
    }
}

package no.difi.meldingsutveksling.serviceregistry.krr;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PersonerResponse {
    @JsonProperty(value = "personer")
    private List<PersonResource> personList = new ArrayList<>();

    public void addPerson(PersonResource person) {
        personList.add(person);
    }

    @JsonIgnore
    public boolean isEmpty() {
        return personList.isEmpty();
    }

    @JsonIgnore
    public List<PersonResource> getPersons() {
        return personList;
    }
}

package no.difi.meldingsutveksling.serviceregistry.krr;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DSFResponse {
    @JsonProperty(value = "personer")
    private List<DSFResource> personList = new ArrayList<>();

    public void addPerson(DSFResource dsfResource) {
        personList.add(dsfResource);
    }

    @JsonIgnore
    public boolean isEmpty() {
        return personList.isEmpty();
    }

    @JsonIgnore
    public List<DSFResource> getPersons() {
        return personList;
    }
}

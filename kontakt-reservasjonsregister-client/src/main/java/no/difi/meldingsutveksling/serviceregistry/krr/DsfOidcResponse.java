package no.difi.meldingsutveksling.serviceregistry.krr;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DsfOidcResponse {

    @JsonProperty(value = "personer")
    private List<DsfResource> personList = new ArrayList<>();

}

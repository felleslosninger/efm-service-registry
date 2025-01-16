package no.difi.meldingsutveksling.serviceregistry.krr;

import no.difi.meldingsutveksling.serviceregistry.config.SRConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

@JsonTest
@ContextConfiguration(classes = SRConfig.class)
@ActiveProfiles("itest")
public class PersonResourceJsonTest {

    @Autowired
    private JacksonTester<PersonResource> json;

    @Test
    void deserialize_JsonWithRequiredFields_ShouldSucceed() throws IOException {
        var jsonValue = "{\"personidentifikator\":\"denpersonen\",\"reservasjon\":\"allrights\", \"status\": \"ok\"}";
        var parsedPerson = json.parse(jsonValue);
        assertEquals("denpersonen", parsedPerson.getObject().getPersonIdentifier());
    }

    @Test
    public void deserialize_JsonHasUnknownField_ShouldPass() throws IOException {
        var jsonValue = "{\"personidentifikator\":\"denpersonen\",\"reservasjon\":\"allrights\", \"status\": \"ok\", \"reservasjonstidspunkt\":\"tidspunkt\"}";
        var parsedPerson = json.parse(jsonValue);
        assertEquals("denpersonen", parsedPerson.getObject().getPersonIdentifier());
    }
}

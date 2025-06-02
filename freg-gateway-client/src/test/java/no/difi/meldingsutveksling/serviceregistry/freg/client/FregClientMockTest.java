package no.difi.meldingsutveksling.serviceregistry.freg.client;

import no.difi.meldingsutveksling.serviceregistry.freg.domain.FregGatewayEntity;
import no.difi.meldingsutveksling.serviceregistry.freg.mock.FregClientMock;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class FregClientMockTest {

    private FregClientMock client;

    @BeforeEach
    void init(){
        client = new FregClientMock();
    }

    @Test
    public void getPersonAddress_returns_mocked_person_address() {
        String pid = "123-pid";

        Optional<FregGatewayEntity.Address.Response> actual = client.getPersonAdress(pid);

        assertTrue(actual.isPresent(), "The mock should return an optional with value");
        assertEquals(pid, actual.get().getPersonIdentifikator(), "The mock returned should have the same pid as parameter");
        assertNotNull(actual.get().getPostadresse(), "The mock returned should have a postal address");
        assertNotNull(actual.get().getNavn(), "The mock returned should have a name");
    }
}

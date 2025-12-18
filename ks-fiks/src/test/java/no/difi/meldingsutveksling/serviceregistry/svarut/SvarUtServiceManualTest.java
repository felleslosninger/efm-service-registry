package no.difi.meldingsutveksling.serviceregistry.svarut;

import no.difi.meldingsutveksling.serviceregistry.config.ServiceregistryProperties;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;

@EnableConfigurationProperties(ServiceregistryProperties.class)
@SpringBootTest(classes = {
        SvarUtService.class,
        TokenService.class,
        SvarUtClient.class,
},
        properties = {
                "difi.move.fiks.oidc.audience=https://test.maskinporten.no/",
                "difi.move.fiks.oidc.url=https://test.maskinporten.no/token",
                "difi.move.fiks.svarut.base-url=https://test.svarut.ks.no/api/v2",
                "difi.move.fiks.oidc.client-id=MOVE_IP_991825827",

                "difi.move.fiks.svarut.integrasjon-id=changeit",
                "difi.move.fiks.svarut.integrasjon-passord=changeit",
                "difi.move.fiks.oidc.keystore.alias=changeit",
                "difi.move.fiks.oidc.keystore.password=changeit",
                "difi.move.fiks.oidc.keystore.path=changeit",
        })
public class SvarUtServiceManualTest {

    @Autowired
    private SvarUtService service;

    @ParameterizedTest
    @ValueSource(strings = {
            "910229028", // Sømådalen og Bessaker Revisjon
            "991825827", // Digdir
            "910568639", // Ramfjordbotn og Åndalsnes
            "910569899", // Oppaker og Passebekk

    })
    @Disabled("Run manually only")
    public void hasSvarUtAdressering(String orgnr) throws SvarUtClientException {

        service.hasSvarUtAdressering(orgnr, null);
    }
}

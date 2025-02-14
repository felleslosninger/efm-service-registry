package no.difi.meldingsutveksling.serviceregistry.freg;

import no.difi.meldingsutveksling.serviceregistry.config.ServiceregistryProperties;
import no.difi.meldingsutveksling.serviceregistry.freg.client.DefaultFregGatewayClient;
import no.difi.meldingsutveksling.serviceregistry.freg.config.FregGatewayConfig;
import no.difi.meldingsutveksling.serviceregistry.freg.mock.FregClientMock;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

public class ConditionalBeansTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(FregClientMock.class)
            .withUserConfiguration(ServiceregistryProperties.class)
            .withUserConfiguration(DefaultFregGatewayClient.class)
            .withUserConfiguration(FregGatewayConfig.class);

    @Test
    void useMock_When_FregEnabledIsFalse() {

        contextRunner.withPropertyValues("difi.move.freg.enabled=false")
                .run(context -> {
                    assertThat(context).hasSingleBean(FregClientMock.class);
                    assertThat(context).doesNotHaveBean(DefaultFregGatewayClient.class);
                });
    }

    @Test
    void useDefaultFregGatewayClient_When_FregEnabledIsTrue() {

        contextRunner.withPropertyValues("difi.move.freg.enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(DefaultFregGatewayClient.class);
                    assertThat(context).doesNotHaveBean(FregClientMock.class);
                });
    }
}

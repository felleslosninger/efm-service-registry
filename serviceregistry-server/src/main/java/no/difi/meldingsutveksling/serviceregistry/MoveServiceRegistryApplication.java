package no.difi.meldingsutveksling.serviceregistry;

import com.netflix.appinfo.HealthCheckHandler;
import com.netflix.appinfo.InstanceInfo;
import no.difi.meldingsutveksling.serviceregistry.config.ServiceregistryProperties;
import no.difi.move.common.config.SpringCloudProtocolResolver;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.netflix.eureka.EurekaClientAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.ConfigurableEnvironment;

@SpringBootApplication
@EnableConfigurationProperties({ServiceregistryProperties.class})
public class MoveServiceRegistryApplication {


    public static void main(String[] args) {
        new SpringApplicationBuilder(MoveServiceRegistryApplication.class)
                .initializers(new SpringCloudProtocolResolver())
                .run(args);
    }

}

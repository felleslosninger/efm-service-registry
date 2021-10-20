package no.difi.meldingsutveksling.serviceregistry.krr;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.serviceregistry.config.ServiceregistryProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.Optional;

@Component
@Slf4j
@Profile({"dev", "staging", "yt"})
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DsfClientWithMock extends DefaultDsfClient {

    public DsfClientWithMock(ObjectMapper objectMapper, ServiceregistryProperties properties) {
        super(objectMapper, properties);
    }

    @Override
    public Optional<DsfResource> getDSFResource(LookupParameters params, URI endpointUri) throws KontaktInfoException {
        return super.getDSFResource(params, endpointUri)
            .or(() -> Optional.of(DsfResource.builder()
                .personIdentifier(params.getIdentifier())
                .name("Raffen")
                .street("Portveien 2")
                .postAddress("0468 Oslo")
                .country("Norge")
                .build()));
    }

}

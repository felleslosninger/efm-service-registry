package no.difi.meldingsutveksling.serviceregistry.fiks.io;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.serviceregistry.CacheConfig;
import no.difi.meldingsutveksling.serviceregistry.SRRequestScope;
import no.difi.meldingsutveksling.serviceregistry.config.ServiceregistryProperties;
import no.difi.meldingsutveksling.serviceregistry.exceptions.EntityNotFoundException;
import no.difi.meldingsutveksling.serviceregistry.exceptions.ServiceRegistryException;
import no.ks.fiks.fiksio.client.api.katalog.model.KatalogKonto;
import no.ks.fiks.io.client.model.Konto;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "difi.move.fiks.io.enable", havingValue = "true")
public class FiksIoService {

    private final ServiceregistryProperties props;
    private final SRRequestScope requestScope;
    private WebClient wc;

    @PostConstruct
    public void init() {
        wc = WebClient.builder()
                .baseUrl(props.getFiks().getIo().getEndpointUrl())
                .defaultHeader("IntegrasjonId", props.getFiks().getIo().getIntegrasjonId())
                .defaultHeader("IntegrasjonPassord", props.getFiks().getIo().getIntegrasjonPassord())
                .build();
    }

    @Cacheable(CacheConfig.FIKSIO_CACHE)
    public Optional<Konto> lookup(String identifier) {
        var res =  wc.get()
                .uri("/fiks-io/katalog/api/v1/kontoer/{identifier}", identifier)
                .header("Authorization", "Bearer " + requestScope.getToken().getTokenValue())
                .retrieve()
                .onStatus(HttpStatusCode::isError, r -> {
                    if (r.statusCode().equals(HttpStatus.NOT_FOUND)) {
                        return Mono.error(new EntityNotFoundException(identifier));
                    } else if (r.statusCode().equals(HttpStatus.UNAUTHORIZED)) {
                        return r.createException()
                                .flatMap(ex -> Mono.error(new AccessDeniedException(ex.getMessage(), ex)));
                    } else {
                        return r.createException()
                                .flatMap(ex -> Mono.error(new ServiceRegistryException(ex)));
                    }
                })
                .bodyToMono(KatalogKonto.class)
                .block(Duration.ofSeconds(5));

        return res != null
                ? Optional.of(Konto.fromKatalogModel(res))
                : Optional.empty();
    }
}


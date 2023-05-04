package no.difi.meldingsutveksling.serviceregistry.service.krr;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.serviceregistry.CacheConfig;
import no.difi.meldingsutveksling.serviceregistry.config.ServiceregistryProperties;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.util.retry.Retry;

import javax.annotation.PostConstruct;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class PrintService {
    private final ServiceregistryProperties properties;
    private WebClient webClient;

    @PostConstruct
    public void init() {
        webClient = WebClient.create(properties.getKrr().getPrintUrl());
    }

    @Cacheable(CacheConfig.CACHE_KRR_PRINT)
    public PrintResponse getPrintDetails() {
        return webClient.get()
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(PrintResponse.class)
                .retryWhen(Retry.fixedDelay(10, Duration.ofSeconds(3)))
                .block(Duration.ofSeconds(30));
    }
}

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
        String url = properties.getKrr().getPrintUrl();
        System.out.println("Initializing WebClient with URL: " + url);
        webClient = WebClient.create(url);
    }

    @Cacheable(CacheConfig.CACHE_KRR_PRINT)
    public PrintResponse getPrintDetails() {
        System.out.println("Entering getPrintDetails method.");

        PrintResponse response = null;
        try {
            System.out.println("Fregurl: " + properties.getFreg().getEndpointURL());
            System.out.println("Initiating webClient request.");
            System.out.println("WebClient url: " + properties.getKrr().getPrintUrl());
            response = webClient.get()
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(PrintResponse.class)
                    .retryWhen(Retry.fixedDelay(10, Duration.ofSeconds(3)))
                    .block(Duration.ofSeconds(30));
            System.out.println("WebClient request completed successfully.");
        } catch (Exception e) {
            System.out.println("Exception occurred during webClient request: " + e.getMessage());
            throw e; // Re-throw the exception after logging it
        }

        System.out.println("Returning response from getPrintDetails method.");
        return response;
    }
}
package no.difi.meldingsutveksling.serviceregistry.svarut;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.serviceregistry.config.ServiceregistryProperties;
import no.difi.move.common.oauth.AuthenticationType;
import no.difi.move.common.oauth.JwtTokenClient;
import no.difi.move.common.oauth.JwtTokenInput;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import no.difi.move.common.oauth.JwtTokenConfig;

import java.util.List;

import static no.difi.meldingsutveksling.serviceregistry.CacheConfig.KSFIKS_CACHE;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final ServiceregistryProperties properties;
    private JwtTokenClient jwtTokenClient;
    private JwtTokenInput jwtTokenInput;

    @PostConstruct
    public void init() {
        var config = new JwtTokenConfig()
                .setClientId(properties.getFiks().getOidc().getClientId())
                .setTokenUri(properties.getFiks().getOidc().getUrl().toString())
                .setAudience(properties.getFiks().getOidc().getAudience())
                .setKeystore(properties.getFiks().getOidc().getKeystore())
                .setScopes(List.of("ks:fiks"))
                .setAuthenticationType(AuthenticationType.CERTIFICATE);

        jwtTokenClient = new JwtTokenClient(config);
        jwtTokenInput = new JwtTokenInput();
    }

    @Cacheable(cacheNames = {KSFIKS_CACHE})
    public String fetchToken() {
        return jwtTokenClient.fetchToken(jwtTokenInput, null).getAccessToken();
    }
}

package no.difi.meldingsutveksling.serviceregistry.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.domain.Iso6523;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationService {

    public Jwt getToken(Authentication auth) {
        JwtAuthenticationToken jwtToken = (JwtAuthenticationToken) auth;
        return jwtToken.getToken();
    }

    public Iso6523 getAuthorizedClientIdentifier(Authentication auth, HttpServletRequest request) {
        JwtAuthenticationToken token = (JwtAuthenticationToken) auth;
        log.trace("JwtAuthenticationToken: {}", token);
        log.trace("token value: {}", token.getToken());
        Map<String, Object> consumer = token.getToken().getClaimAsMap("consumer");
        String identifier = (String) consumer.get("ID");

        if (!Iso6523.isValid(identifier)) {
            throw new AccessDeniedException(String.format("Unauthorized lookup from %s - consumer.ID claim in token not present, or does not match ISO6523", request.getRemoteAddr()));
        }
        return Iso6523.parse(identifier);
    }
}

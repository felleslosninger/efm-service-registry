package no.difi.meldingsutveksling.serviceregistry.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.domain.ICD;
import no.difi.meldingsutveksling.domain.Iso6523;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private static final Pattern ISO6523_PATTERN = Pattern.compile("^([0-9]{4}:)?([0-9]{9})?(?::)?([0-9]{9})?$");

    public Jwt getToken(Authentication auth) {
        JwtAuthenticationToken jwtToken = (JwtAuthenticationToken) auth;
        return jwtToken.getToken();
    }

    public Iso6523 getAuthorizedClientIdentifier(Authentication auth, HttpServletRequest request) {
        JwtAuthenticationToken token = (JwtAuthenticationToken) auth;
        Map<String, Object> consumer = token.getToken().getClaimAsMap("consumer");
        Matcher matcher = ISO6523_PATTERN.matcher((String) consumer.get("ID"));
        if (!matcher.matches()) {
            throw new AccessDeniedException(String.format("Unauthorized lookup from %s - consumer.ID claim in token not present, or does not match ISO6523", request.getRemoteAddr()));
        }
        if (StringUtils.hasText(matcher.group(1))) {
            return Iso6523.parse((String) consumer.get("ID"));
        }
        return Iso6523.of(ICD.NO_ORG, matcher.group(2));
    }

}

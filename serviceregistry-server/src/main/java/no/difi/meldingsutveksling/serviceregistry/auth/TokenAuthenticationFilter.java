package no.difi.meldingsutveksling.serviceregistry.auth;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "difi.move.auth.enable", havingValue = "true")
public class TokenAuthenticationFilter extends OncePerRequestFilter {

    private final TokenValidator tokenValidator;

    public TokenAuthenticationFilter(TokenValidator tokenValidator) {
        this.tokenValidator = tokenValidator;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        String accessToken = resolveToken(request);
        TokenValidator.Result validationResult
                = null != accessToken
                ? tokenValidator.validate(accessToken)
                : null;
        if (null != validationResult && validationResult.isValid()) {
            setOauthAuthentication(validationResult.getClaims().getClaims());
        }
        chain.doFilter(request, response);
    }

    private void setOauthAuthentication(Map<String, Object> claims) {
        Object clientId = claims.getOrDefault("client_orgno", null);
        List<String> scopes = Arrays.asList(((String) claims.get("scope")).split(" "));
        OAuth2Request request = new OAuth2Request(null, (String) clientId, null, true, new HashSet<>(scopes), null, null, null, null);
        OAuth2Authentication authentication = new OAuth2Authentication(request, null);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private String resolveToken(HttpServletRequest req) {
        String bearerToken = req.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

}

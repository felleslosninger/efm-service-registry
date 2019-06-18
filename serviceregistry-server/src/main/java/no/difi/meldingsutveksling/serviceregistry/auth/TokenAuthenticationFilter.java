package no.difi.meldingsutveksling.serviceregistry.auth;

import com.nimbusds.jwt.JWTClaimsSet;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.GenericFilterBean;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@ConditionalOnProperty(name = "difi.move.auth.enable", havingValue = "true")
@Configuration
public class TokenAuthenticationFilter extends GenericFilterBean {

    private final TokenValidator tokenValidator;

    public TokenAuthenticationFilter(TokenValidator tokenValidator) {
        this.tokenValidator = tokenValidator;
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
            throws IOException, ServletException {
        final HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;
        final String accessToken = resolveToken(httpRequest);
        TokenValidator.Result validationResult
                = null != accessToken
                ? tokenValidator.validate(accessToken)
                : null;
        if (null != validationResult) {
            JWTClaimsSet claimsSet = validationResult.getClaims();
            Map<String, Object> claims = claimsSet.getClaims();
            Object client_orgno = claims.getOrDefault("client_orgno", null);
            List<String> scopes = Arrays.asList(((String) claims.get("scope")).split(" "));
            OpenIdConnectAuthenticationToken authentication
                    = new OpenIdConnectAuthenticationToken(client_orgno, scopes);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        chain.doFilter(servletRequest, servletResponse);
    }

    private String resolveToken(HttpServletRequest req) {
        String bearerToken = req.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}

package no.difi.meldingsutveksling.serviceregistry.auth;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.RemoteJWKSet;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.BadJWTException;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.serviceregistry.config.ServiceregistryProperties;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URL;
import java.text.ParseException;

@Slf4j
@Component
public class TokenValidator {

    /**
     * The signing algorithm.
     */
    private static final JWSAlgorithm SIGNING_ALGORITHM = JWSAlgorithm.RS256;

    /**
     * The expected token issuer (from configuration)
     */
    private final String tokenIssuer;

    /**
     * The JWKS URI (from configuration)
     */
    private final String jwksUri;

    public TokenValidator(ServiceregistryProperties properties) {
        ServiceregistryProperties.Auth authorizationProperties = properties.getAuth();
        this.tokenIssuer = authorizationProperties.getIssuer();
        this.jwksUri = authorizationProperties.getJwksUri();
    }

    public Result validate(String token) throws IOException {
        JWSObject jwsToken;
        try {
            jwsToken = JWSObject.parse(token);
        } catch (ParseException e) {
            log.error("Invalid authorization token.");
            return new Result(false, null);
        }
        String algorithm = jwsToken.getHeader().getAlgorithm().getName().toUpperCase();
        if (!validateSigningAlgorithm(algorithm)) {
            return new Result(false, null);
        }
        ConfigurableJWTProcessor jwtProcessor = new DefaultJWTProcessor();
        JWKSource keySource = new RemoteJWKSet(new URL(jwksUri));
        JWSKeySelector keySelector = new JWSVerificationKeySelector(SIGNING_ALGORITHM, keySource);
        jwtProcessor.setJWSKeySelector(keySelector);
        SecurityContext context = null;
        try {
            JWTClaimsSet claims = jwtProcessor.process(token, context);
            return new Result(validateTokenIssuer(claims), claims);
        } catch (ParseException e) {
            log.error("Unable to parse the authorization token", e);
        }catch (BadJOSEException e) {
            log.error("Bad JOSE encountered", e);
        } catch (JOSEException e) {
            log.error("Unable to process the authorization token signature", e);
        }
        return new Result(false, null);
    }

    @Value
    public class Result {
        boolean valid;
        JWTClaimsSet claims;
    }

    /**
     * Check whether the token has been released by the expected issuer
     */
    private Boolean validateTokenIssuer(JWTClaimsSet claims) {
        String issuer = claims.getIssuer();
        if (issuer == null) {
            log.error("The authorization token doesn't have an issuer (iss)");
            return false;
        }
        if (issuer.toLowerCase().equals(this.tokenIssuer)) {
            return true;
        }
        log.error("The authorization token issuer '{}' doesn't match the expected issuer '{}'", issuer, this.tokenIssuer);
        return false;
    }

    /**
     * Validate the algorithm in the JWS header.
     *
     * @param algorithm Name of token algorithm.
     * @return True if algorithm is matching SIGNING_ALGORITHM.
     */
    private boolean validateSigningAlgorithm(String algorithm) {
        if (SIGNING_ALGORITHM.getName().equals(algorithm.toUpperCase())) {
            return true;
        }
        log.error("The authorization token is signed with an invalid algoritm: {}", algorithm);
        return false;
    }
}

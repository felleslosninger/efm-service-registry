package no.difi.meldingsutveksling.serviceregistry.record;

import lombok.Data;
import org.springframework.security.oauth2.jwt.Jwt;

@Data
public class LookupParameters {
    private final String identifier;
    private Jwt token;

    private LookupParameters(String identifier) {
        this.identifier = identifier;
    }

    public static LookupParameters lookup(String identifier) {
        return new LookupParameters(identifier);
    }

    public LookupParameters token(Jwt token) {
        setToken(token);
        return this;
    }

}

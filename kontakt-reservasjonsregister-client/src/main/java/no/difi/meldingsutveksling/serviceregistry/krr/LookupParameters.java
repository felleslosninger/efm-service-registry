package no.difi.meldingsutveksling.serviceregistry.krr;

import lombok.Data;
import no.difi.meldingsutveksling.domain.PersonIdentifier;
import org.springframework.security.oauth2.jwt.Jwt;

@Data
public class LookupParameters {
    private final PersonIdentifier identifier;
    private Jwt token;

    private LookupParameters(PersonIdentifier identifier) {
        this.identifier = identifier;
    }

    public static LookupParameters lookup(PersonIdentifier identifier) {
        return new LookupParameters(identifier);
    }

    public LookupParameters token(Jwt token) {
        setToken(token);
        return this;
    }

}

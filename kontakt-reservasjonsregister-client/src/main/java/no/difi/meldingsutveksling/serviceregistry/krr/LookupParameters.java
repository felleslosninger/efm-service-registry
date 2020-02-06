package no.difi.meldingsutveksling.serviceregistry.krr;

import lombok.Data;

@Data
public class LookupParameters {
    private final String identifier;
    private String token;

    private LookupParameters(String identifier) {
        this.identifier = identifier;
    }

    public static LookupParameters lookup(String identifier) {
        return new LookupParameters(identifier);
    }

    public LookupParameters token(String token) {
        setToken(token);
        return this;
    }

}

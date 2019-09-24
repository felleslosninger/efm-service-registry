package no.difi.meldingsutveksling.serviceregistry.krr;

import lombok.Data;

@Data
public class LookupParameters {
    private final String identifier;
    private String clientOrgnr;
    private String token;

    private LookupParameters(String identifier) {
        this.identifier = identifier;
    }

    public LookupParameters onBehalfOf(String clientOrgnr) {
        setClientOrgnr(clientOrgnr);
        return this;
    }

    public static LookupParameters lookup(String identifier) {
        return new LookupParameters(identifier);
    }

    public LookupParameters token(String token) {
        setToken(token);
        return this;
    }

}

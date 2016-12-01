package no.difi.meldingsutveksling.serviceregistry.krr;

public class LookupParameters {
    private final String identifier;
    private String clientOrgnr;

    public LookupParameters(String identifier) {
        this.identifier = identifier;
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getClientOrgnr() {
        return clientOrgnr;
    }

    public void setClientOrgnr(String clientOrgnr) {
        this.clientOrgnr = clientOrgnr;
    }

    public LookupParameters onBehalfOf(String clientOrgnr) {
        setClientOrgnr(clientOrgnr);
        return this;
    }

    public static LookupParameters lookup(String identifier) {
        final LookupParameters lookupParameters = new LookupParameters(identifier);
        return lookupParameters;
    }
}

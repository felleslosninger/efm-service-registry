package no.difi.meldingsutveksling.serviceregistry.krr;

import lombok.Data;
import no.difi.meldingsutveksling.Notification;

@Data
public class LookupParameters {
    private final String identifier;
    private String clientOrgnr;
    private Notification notification;
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

    public LookupParameters require(Notification obligation) {
        setNotification(obligation);
        return this;
    }

    public LookupParameters token(String token) {
        setToken(token);
        return this;
    }

    public boolean isObligatedToBeNotified() {
        return notification == Notification.OBLIGATED;
    }
}

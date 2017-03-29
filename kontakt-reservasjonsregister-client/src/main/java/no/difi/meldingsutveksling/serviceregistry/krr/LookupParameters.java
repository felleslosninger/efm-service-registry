package no.difi.meldingsutveksling.serviceregistry.krr;

import no.difi.meldingsutveksling.Notification;

public class LookupParameters {
    private final String identifier;
    private String clientOrgnr;
    private Notification notification;
    private String token;

    private LookupParameters(String identifier) {
        this.identifier = identifier;
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getClientOrgnr() {
        return clientOrgnr;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    private void setClientOrgnr(String clientOrgnr) {
        this.clientOrgnr = clientOrgnr;
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

    private void setNotification(Notification notification) {
        this.notification = notification;
    }

    public Notification getNotification() {
        return notification;
    }

    public boolean isObligatedToBeNotified() {
        return notification == Notification.OBLIGATED;
    }
}

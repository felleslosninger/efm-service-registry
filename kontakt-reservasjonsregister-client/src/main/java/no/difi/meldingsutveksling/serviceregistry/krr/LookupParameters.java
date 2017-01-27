package no.difi.meldingsutveksling.serviceregistry.krr;

import no.difi.meldingsutveksling.NotificationObligation;

public class LookupParameters {
    private final String identifier;
    private String clientOrgnr;
    private NotificationObligation notificationObligation;

    private LookupParameters(String identifier) {
        this.identifier = identifier;
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getClientOrgnr() {
        return clientOrgnr;
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

    public LookupParameters require(NotificationObligation obligation) {
        setNotificationObligation(obligation);
        return this;
    }

    private void setNotificationObligation(NotificationObligation notificationObligation) {
        this.notificationObligation = notificationObligation;
    }

    public NotificationObligation getNotificationObligation() {
        return notificationObligation;
    }

    public boolean isObligatedToBeNotified() {
        return notificationObligation == NotificationObligation.OBLIGATED;
    }
}

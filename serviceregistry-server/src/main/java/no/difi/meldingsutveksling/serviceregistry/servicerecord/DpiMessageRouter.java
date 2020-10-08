package no.difi.meldingsutveksling.serviceregistry.servicerecord;

import no.difi.meldingsutveksling.serviceregistry.domain.Notification;
import no.difi.meldingsutveksling.serviceregistry.krr.PersonResource;

import static no.difi.meldingsutveksling.serviceregistry.domain.Notification.OBLIGATED;
import static no.difi.meldingsutveksling.serviceregistry.servicerecord.DpiMessageRouter.TargetRecord.DPI;
import static no.difi.meldingsutveksling.serviceregistry.servicerecord.DpiMessageRouter.TargetRecord.DPV;
import static no.difi.meldingsutveksling.serviceregistry.servicerecord.DpiMessageRouter.TargetRecord.PRINT;

public class DpiMessageRouter {

    public enum TargetRecord {
        DPI,
        PRINT,
        DPV
    }

    /**
     * Determines which route a DPI message should take, based on receiver information from KRR
     * and parameters from the client.
     *
     * @param person resource from KRR
     * @param notification if notification is obligatory
     * @return enum value to determine course of message
     */
    public static TargetRecord route(PersonResource person, Notification notification) {
        if (OBLIGATED.equals(notification)) {
            return checkReservation(person);
        } else {
            return checkActiveMailbox(person);
        }
    }

    private static TargetRecord checkReservation(PersonResource person) {
        if (person.isReserved()) {
            return PRINT;
        }
        return checkNotification(person);
    }

    private static TargetRecord checkNotification(PersonResource person) {
        if (person.isNotifiable()) {
            return checkActiveMailbox(person);
        }
        return PRINT;
    }

    private static TargetRecord checkActiveMailbox(PersonResource person) {
        if (person.hasMailbox() && person.isActive()) {
            return DPI;
        }
        return DPV;
    }

}

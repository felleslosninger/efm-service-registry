package no.difi.meldingsutveksling.serviceregistry.servicerecord;

import no.difi.meldingsutveksling.Notification;
import no.difi.meldingsutveksling.serviceregistry.krr.PersonResource;

import static no.difi.meldingsutveksling.Notification.OBLIGATED;
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
     * @param forcePrint if message should be printed instead of going to DPV
     * @return enum value to determine course of message
     */
    public static TargetRecord route(PersonResource person, Notification notification, boolean forcePrint) {
        if (OBLIGATED.equals(notification)) {
            return checkReservation(person, forcePrint);
        } else {
            return checkActiveMailbox(person, forcePrint);
        }
    }

    private static TargetRecord checkReservation(PersonResource person, boolean forcePrint) {
        if (person.isReserved()) {
            return PRINT;
        }
        return checkNotification(person, forcePrint);
    }

    private static TargetRecord checkNotification(PersonResource person, boolean forcePrint) {
        if (person.isNotifiable()) {
            return checkActiveMailbox(person, forcePrint);
        }
        return checkForcePrint(person, forcePrint);
    }

    private static TargetRecord checkActiveMailbox(PersonResource person, boolean forcePrint) {
        if (person.hasMailbox() && person.isActive()) {
            return DPI;
        }
        return checkForcePrint(person, forcePrint);
    }

    private static TargetRecord checkForcePrint(PersonResource person, boolean forcePrint) {
        if (forcePrint) {
            return PRINT;
        }
        return DPV;
    }
}

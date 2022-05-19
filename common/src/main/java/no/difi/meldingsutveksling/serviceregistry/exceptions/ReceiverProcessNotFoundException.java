package no.difi.meldingsutveksling.serviceregistry.exceptions;

import no.difi.meldingsutveksling.domain.Iso6523;
import no.difi.meldingsutveksling.domain.PartnerIdentifier;

public class ReceiverProcessNotFoundException extends Exception {
    public ReceiverProcessNotFoundException(PartnerIdentifier identifier, String process) {
        super("Process " + process + " not found for receiver " + identifier);
    }
}

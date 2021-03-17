package no.difi.meldingsutveksling.serviceregistry.exceptions;

public class ReceiverProcessNotFoundException extends Exception {
    public ReceiverProcessNotFoundException(String identifier, String process) {
        super("Process " + process + " not found for receiver " + identifier);
    }
}

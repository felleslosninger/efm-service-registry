package no.difi.meldingsutveksling.serviceregistry.mvc;

public class UnknownServiceIdentifierException extends RuntimeException {

    public UnknownServiceIdentifierException(String s) {
        super("Unknown service identifier: " + s);
    }
}

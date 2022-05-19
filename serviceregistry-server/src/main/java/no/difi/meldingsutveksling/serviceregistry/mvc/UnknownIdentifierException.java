package no.difi.meldingsutveksling.serviceregistry.mvc;

public class UnknownIdentifierException extends RuntimeException {

    public UnknownIdentifierException(String s, Throwable t) {
        super(s, t);
    }
}

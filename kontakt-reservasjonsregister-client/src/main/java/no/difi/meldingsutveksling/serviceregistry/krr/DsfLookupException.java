package no.difi.meldingsutveksling.serviceregistry.krr;

public class DsfLookupException extends Exception {

    public DsfLookupException(String message) {
        super(message);
    }

    public DsfLookupException(String message, Throwable e) {
        super(message, e);
    }

}

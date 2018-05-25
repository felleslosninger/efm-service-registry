package no.difi.meldingsutveksling.serviceregistry.service.brreg;

public class BrregNotFoundException extends Exception {

    public BrregNotFoundException(String s) {
        super(s);
    }

    public BrregNotFoundException(String s, Throwable e) {
        super(s, e);
    }
}

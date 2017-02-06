package no.difi.meldingsutveksling.serviceregistry.security;

public class EntitySignerException extends Exception {

    EntitySignerException(String s, Exception e) {
        super(s, e);
    }

    EntitySignerException(Exception e) {
        super(e);
    }
}

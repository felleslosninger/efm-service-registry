package no.difi.meldingsutveksling.serviceregistry.krr;

public class KRRClientException extends Exception {

    KRRClientException(String s, Exception e) {
        super(s, e);
    }

    public KRRClientException(Exception e) {
        super(e);
    }

    KRRClientException(String s) {
        super(s);
    }
}

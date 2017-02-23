package no.difi.meldingsutveksling.serviceregistry.krr;

public class KRRClientException extends Exception {

    KRRClientException(String s, Exception e) {
        super(s, e);
    }

    KRRClientException(String s) {
        super(s);
    }
}

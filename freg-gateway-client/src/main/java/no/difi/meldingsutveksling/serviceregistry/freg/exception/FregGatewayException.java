package no.difi.meldingsutveksling.serviceregistry.freg.exception;

public class FregGatewayException extends Exception {

        public FregGatewayException(String s) {
            super(s);
        }

        public FregGatewayException(String s, Throwable t) {
            super(s, t);
        }
}
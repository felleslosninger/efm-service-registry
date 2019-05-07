package no.difi.meldingsutveksling.serviceregistry;


import no.difi.virksert.client.lang.VirksertClientException;

public class CertificateNotFoundException extends Exception {
    public CertificateNotFoundException(Throwable cause) {
        super(cause);
    }

    public CertificateNotFoundException(String message, VirksertClientException e) {
        super(message, e);
    }
}

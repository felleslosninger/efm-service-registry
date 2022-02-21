package no.difi.meldingsutveksling.serviceregistry.exceptions;

public class NonMatchingCertificatesException extends RuntimeException {

    public NonMatchingCertificatesException(String identifier) {
        super("DPO and DPE certificates from Virksert do not match for identifier "+identifier);
    }

}

package no.difi.meldingsutveksling.serviceregistry.exceptions;

import no.difi.meldingsutveksling.domain.Iso6523;

public class NonMatchingCertificatesException extends RuntimeException {

    public NonMatchingCertificatesException(Iso6523 identifier) {
        super("DPO and DPE certificates from Virksert do not match for identifier "+identifier);
    }

}

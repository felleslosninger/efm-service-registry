package no.difi.meldingsutveksling.serviceregistry.service.ks;

class FiksAdresseServiceException extends RuntimeException {
    FiksAdresseServiceException(String s, Exception e) {
        super(s, e);
    }
}

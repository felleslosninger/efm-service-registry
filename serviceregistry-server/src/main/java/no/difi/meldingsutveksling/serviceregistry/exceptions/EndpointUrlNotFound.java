package no.difi.meldingsutveksling.serviceregistry.exceptions;

import no.difi.vefa.peppol.common.lang.PeppolException;

/**
 * Thrown when the endpoint url to a service record is unknown
 */
public class EndpointUrlNotFound extends Exception {
    public EndpointUrlNotFound(String message, PeppolException cause) {
        super(message, cause);
    }
}

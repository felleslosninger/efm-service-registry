package no.difi.meldingsutveksling.serviceregistry.exceptions;

/**
 * Thrown when the endpoint url to a service record is unknown
 */
public class EndpointUrlNotFound extends Exception {
    public EndpointUrlNotFound(String message, Throwable cause) {
        super(message, cause);
    }
}

package no.difi.meldingsutveksling.serviceregistry;


import network.oxalis.vefa.peppol.common.lang.PeppolException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "No such resource")
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(Throwable cause) {
        super(cause);
    }

    public ResourceNotFoundException(String message, PeppolException cause) {
        super(message, cause);
    }
}

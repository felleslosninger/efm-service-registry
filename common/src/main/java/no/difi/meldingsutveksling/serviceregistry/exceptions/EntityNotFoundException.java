package no.difi.meldingsutveksling.serviceregistry.exceptions;

public class EntityNotFoundException extends RuntimeException {
    public EntityNotFoundException(Throwable cause) {
        super(cause);
    }

    public EntityNotFoundException(String message) {
        super(String.format("Entity with identifier '%s' not found.", message));
    }
}

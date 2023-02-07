package no.difi.meldingsutveksling.serviceregistry.freg.exception;

import org.springframework.web.client.HttpClientErrorException;

public class NotFoundInMfGatewayException extends Exception {

    public NotFoundInMfGatewayException(HttpClientErrorException errorException) {
        super("User not found in MF" + errorException);
    }
}
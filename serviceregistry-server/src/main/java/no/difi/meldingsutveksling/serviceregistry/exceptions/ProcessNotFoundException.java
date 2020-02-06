package no.difi.meldingsutveksling.serviceregistry.exceptions;

public class ProcessNotFoundException extends Exception{
    public ProcessNotFoundException(String processIdentifier) {
        super("Given processIdentifier "+ processIdentifier + " not registered");
    }

}

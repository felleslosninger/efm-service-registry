package no.difi.meldingsutveksling.serviceregistry.exceptions;

public class ProcessNotFoundException extends Exception{
    public ProcessNotFoundException(String message) {super("Given processIdentifier "+ message + " not registered");}

}

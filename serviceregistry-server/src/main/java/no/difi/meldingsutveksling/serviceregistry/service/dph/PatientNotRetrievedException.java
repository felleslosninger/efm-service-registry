package no.difi.meldingsutveksling.serviceregistry.service.dph;

import no.difi.meldingsutveksling.serviceregistry.freg.exception.FregGatewayException;

public class PatientNotRetrievedException extends FregGatewayException {

    public PatientNotRetrievedException() {
        super("Not able to retrieve patient information");
    }

    public PatientNotRetrievedException(Throwable t) {
        super("Not able to retrieve patient information", t);
    }

}

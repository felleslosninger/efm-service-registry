package no.difi.meldingsutveksling.serviceregistry.servicerecord;

import no.difi.meldingsutveksling.ptp.KontaktInfo;
import no.difi.meldingsutveksling.serviceregistry.model.ServiceIdentifier;
import org.springframework.core.env.Environment;

public class SikkerDigitalPostServiceRecord extends ServiceRecord {
    private final String orgnrPostkasse;
    private final String postkasseAdresse;

    public SikkerDigitalPostServiceRecord(Environment e, KontaktInfo kontaktInfo, ServiceIdentifier serviceIdentifier, String organisationNumber) {
        super(e, kontaktInfo.getCertificate(), serviceIdentifier, organisationNumber);
        orgnrPostkasse = kontaktInfo.getOrgnrPostkasse();
        postkasseAdresse = kontaktInfo.getPostkasseAdresse();
    }

    @Override
    public String getEndPointURL() {
        return environment.getProperty("krr.endpointURL");
    }

    public String getOrgnrPostkasse() {
        return orgnrPostkasse;
    }

    public String getPostkasseAdresse() {
        return postkasseAdresse;
    }
}

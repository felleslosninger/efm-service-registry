package no.difi.meldingsutveksling.serviceregistry.servicerecord;

import no.difi.meldingsutveksling.ptp.KontaktInfo;
import no.difi.meldingsutveksling.serviceregistry.config.ServiceregistryProperties;
import no.difi.meldingsutveksling.serviceregistry.model.ServiceIdentifier;

public class SikkerDigitalPostServiceRecord extends ServiceRecord {

    private final String orgnrPostkasse;
    private final String postkasseAdresse;

    public SikkerDigitalPostServiceRecord(ServiceregistryProperties properties, KontaktInfo kontaktInfo, ServiceIdentifier serviceIdentifier, String organisationNumber) {
        super(properties, kontaktInfo.getCertificate(), serviceIdentifier, organisationNumber);
        orgnrPostkasse = kontaktInfo.getOrgnrPostkasse();
        postkasseAdresse = kontaktInfo.getPostkasseAdresse();
    }

    @Override
    public String getEndPointURL() {
        return properties.getDpi().getEndpointURL().toString();
    }

    public String getOrgnrPostkasse() {
        return orgnrPostkasse;
    }

    public String getPostkasseAdresse() {
        return postkasseAdresse;
    }
}

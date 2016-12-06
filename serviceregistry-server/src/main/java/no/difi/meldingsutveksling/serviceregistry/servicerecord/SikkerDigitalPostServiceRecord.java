package no.difi.meldingsutveksling.serviceregistry.servicerecord;

import no.difi.meldingsutveksling.ptp.KontaktInfo;
import no.difi.meldingsutveksling.ptp.PostAddress;
import no.difi.meldingsutveksling.serviceregistry.config.ServiceregistryProperties;
import no.difi.meldingsutveksling.serviceregistry.model.ServiceIdentifier;

public class SikkerDigitalPostServiceRecord extends ServiceRecord {

    private final String orgnrPostkasse;
    private final String postkasseAdresse;
    private final String mobilnummer;
    private final boolean kanVarsles;
    private final String epostAdresse;
    private final boolean fysiskPost;
    private final PostAddress postAddress;
    private final PostAddress returnAddress;

    public SikkerDigitalPostServiceRecord(ServiceregistryProperties properties, KontaktInfo kontaktInfo, ServiceIdentifier serviceIdentifier, String organisationNumber, PostAddress postAddress, PostAddress returnAddress) {
        super(properties, kontaktInfo.getCertificate(), serviceIdentifier, organisationNumber);
        orgnrPostkasse = kontaktInfo.getOrgnrPostkasse();
        postkasseAdresse = kontaktInfo.getPostkasseAdresse();
        kanVarsles = kontaktInfo.isNotifiable();
        epostAdresse = kontaktInfo.getEpostadresse();
        mobilnummer = kontaktInfo.getMobiltelefonnummer();
        fysiskPost = kontaktInfo.isReservert();
        this.postAddress = postAddress;
        this.returnAddress = returnAddress;
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

    public String getMobilnummer() {
        return mobilnummer;
    }

    public boolean getKanVarsles() {
        return kanVarsles;
    }

    public String getEpostAdresse() {
        return epostAdresse;
    }

    public boolean isFysiskPost() {
        return fysiskPost;
    }

    public PostAddress getPostAddress() {
        return postAddress;
    }

    public PostAddress getReturnAddress() {
        return returnAddress;
    }
}

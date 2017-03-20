package no.difi.meldingsutveksling.serviceregistry.servicerecord;

import no.difi.meldingsutveksling.ptp.KontaktInfo;
import no.difi.meldingsutveksling.ptp.PostAddress;
import no.difi.meldingsutveksling.serviceregistry.config.ServiceregistryProperties;
import no.difi.meldingsutveksling.serviceregistry.krr.PersonResource;
import no.difi.meldingsutveksling.serviceregistry.model.ServiceIdentifier;

import static no.difi.meldingsutveksling.serviceregistry.krr.PersonResource.Reservasjon.JA;
import static no.difi.meldingsutveksling.serviceregistry.krr.PersonResource.Varslingsstatus.KAN_VARSLES;

public class SikkerDigitalPostServiceRecord extends ServiceRecord {

    private final ServiceregistryProperties properties;
    private final String orgnrPostkasse;
    private final String postkasseAdresse;
    private final String mobilnummer;
    private final boolean kanVarsles;
    private final String epostAdresse;
    private final boolean fysiskPost;
    private final PostAddress postAddress;
    private final PostAddress returnAddress;

    public SikkerDigitalPostServiceRecord(ServiceregistryProperties properties, KontaktInfo kontaktInfo, ServiceIdentifier serviceIdentifier, String organisationNumber, PostAddress postAddress, PostAddress returnAddress) {
        super(kontaktInfo.getCertificate(), serviceIdentifier, organisationNumber);
        this.properties = properties;
        orgnrPostkasse = kontaktInfo.getOrgnrPostkasse();
        postkasseAdresse = kontaktInfo.getPostkasseAdresse();
        kanVarsles = kontaktInfo.isNotifiable();
        epostAdresse = kontaktInfo.getEpostadresse();
        mobilnummer = kontaktInfo.getMobiltelefonnummer();
        fysiskPost = kontaktInfo.isReservert();
        this.postAddress = postAddress;
        this.returnAddress = returnAddress;
    }

    public SikkerDigitalPostServiceRecord(ServiceregistryProperties properties, PersonResource personResource,
                                          ServiceIdentifier serviceIdentifier, String organisationNumber,
                                          PostAddress postAddress, PostAddress returnAddress) {
        super(properties, personResource.getCertificate(), serviceIdentifier, organisationNumber);
        orgnrPostkasse = personResource.getDigitalPost().getPostkasseleverandoeradresse();
        postkasseAdresse = personResource.getDigitalPost().getPostkasseadresse();

        kanVarsles = KAN_VARSLES.name().equals(personResource.getAlertStatus());
        epostAdresse = personResource.getContactInfo().getEmail();
        mobilnummer = personResource.getContactInfo().getMobile();
        fysiskPost = JA.name().equals(personResource.getReserved());

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

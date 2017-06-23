package no.difi.meldingsutveksling.serviceregistry.servicerecord;

import lombok.Data;
import no.difi.meldingsutveksling.serviceregistry.krr.PostAddress;
import no.difi.meldingsutveksling.serviceregistry.config.ServiceregistryProperties;
import no.difi.meldingsutveksling.serviceregistry.krr.PersonResource;
import no.difi.meldingsutveksling.serviceregistry.model.ServiceIdentifier;

import static no.difi.meldingsutveksling.serviceregistry.krr.PersonResource.Reservasjon.JA;
import static no.difi.meldingsutveksling.serviceregistry.krr.PersonResource.Varslingsstatus.KAN_VARSLES;

@Data
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

    public SikkerDigitalPostServiceRecord(ServiceregistryProperties properties, PersonResource personResource,
                                          ServiceIdentifier serviceIdentifier, String organisationNumber,
                                          PostAddress postAddress, PostAddress returnAddress) {
        super(personResource.getCertificate(), serviceIdentifier, organisationNumber);
        this.properties = properties;

        if (personResource.hasMailbox()) {
            orgnrPostkasse = personResource.getDigitalPost().getPostkasseleverandoeradresse();
            postkasseAdresse = personResource.getDigitalPost().getPostkasseadresse();
        } else {
            orgnrPostkasse = personResource.getPrintPostkasseLeverandorAdr();
            postkasseAdresse = null;
        }

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

}

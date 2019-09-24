package no.difi.meldingsutveksling.serviceregistry.servicerecord;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import no.difi.meldingsutveksling.serviceregistry.config.ServiceregistryProperties;
import no.difi.meldingsutveksling.serviceregistry.krr.PersonResource;
import no.difi.meldingsutveksling.serviceregistry.krr.PostAddress;
import no.difi.meldingsutveksling.serviceregistry.model.ServiceIdentifier;

import static no.difi.meldingsutveksling.serviceregistry.krr.PersonResource.Varslingsstatus.KAN_VARSLES;

@Getter
@Setter
@ToString
@EqualsAndHashCode(callSuper = true)
public class SikkerDigitalPostServiceRecord extends ServiceRecord {

    @JsonIgnore
    private final ServiceregistryProperties properties;
    private final String orgnrPostkasse;
    private final String postkasseAdresse;
    private final String mobilnummer;
    private final boolean kanVarsles;
    private final String epostAdresse;
    private final boolean fysiskPost;
    private final PostAddress postAddress;
    private final PostAddress returnAddress;

    public SikkerDigitalPostServiceRecord(boolean isFysiskPost,
                                          ServiceregistryProperties properties,
                                          PersonResource personResource,
                                          ServiceIdentifier serviceIdentifier,
                                          String organisationNumber,
                                          PostAddress postAddress,
                                          PostAddress returnAddress) {
        super(serviceIdentifier, organisationNumber, properties.getDpi().getEndpointURL().toString());
        setPemCertificate(personResource.getCertificate());
        this.properties = properties;

        fysiskPost = isFysiskPost;
        if (isFysiskPost) {
            orgnrPostkasse = personResource.getPrintPostkasseLeverandorAdr();
            postkasseAdresse = null;
        } else {
            orgnrPostkasse = personResource.getDigitalPost().getPostkasseleverandoeradresse();
            postkasseAdresse = personResource.getDigitalPost().getPostkasseadresse();
        }

        kanVarsles = KAN_VARSLES.name().equals(personResource.getAlertStatus());
        if (personResource.getContactInfo() != null) {
            epostAdresse = personResource.getContactInfo().getEmail();
            mobilnummer = personResource.getContactInfo().getMobile();
        } else {
            epostAdresse = null;
            mobilnummer = null;
        }

        this.postAddress = postAddress;
        this.returnAddress = returnAddress;
    }

}

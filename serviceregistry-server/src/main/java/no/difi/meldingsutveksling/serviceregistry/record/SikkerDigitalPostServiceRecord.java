package no.difi.meldingsutveksling.serviceregistry.record;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import no.difi.meldingsutveksling.serviceregistry.domain.Process;
import no.difi.meldingsutveksling.serviceregistry.domain.ServiceIdentifier;
import no.difi.meldingsutveksling.serviceregistry.krr.PersonResource;
import no.difi.meldingsutveksling.serviceregistry.krr.PostAddress;

import static no.difi.meldingsutveksling.serviceregistry.krr.PersonResource.Varslingsstatus.KAN_VARSLES;

@Getter
@Setter
@ToString
@EqualsAndHashCode(callSuper = true)
public class SikkerDigitalPostServiceRecord extends ServiceRecord {

    private final String orgnrPostkasse;
    private final String postkasseAdresse;
    private final String mobilnummer;
    private final boolean kanVarsles;
    private final String epostAdresse;
    private final boolean fysiskPost;
    private final PostAddress postAddress;
    private final PostAddress returnAddress;

    public SikkerDigitalPostServiceRecord(String identifier,
                                          Process process,
                                          PersonResource personResource,
                                          String endpointUrl,
                                          boolean isFysiskPost,
                                          PostAddress postAddress,
                                          PostAddress returnAddress) {
        super(ServiceIdentifier.DPI, identifier, process, endpointUrl);
        setPemCertificate(personResource.getCertificate());

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

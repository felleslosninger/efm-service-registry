package no.difi.meldingsutveksling.ptp;

import com.google.common.base.MoreObjects;
import no.difi.ptp.sikkerdigitalpost.HentPersonerRespons;

public class KontaktInfo {
    private final PersonKontaktInfoMapper.MailboxProvider providerDetails;
    private final PersonKontaktInfoMapper.PersonDetails personDetails;
    private PrintProviderDetails printDetails;

    KontaktInfo(PersonKontaktInfoMapper.MailboxProvider providerDetails, PersonKontaktInfoMapper.PersonDetails personDetails) {
        this.providerDetails = providerDetails;
        this.personDetails = personDetails;
    }

    public String getCertificate() {
        if (canReceiveDigitalPost()) {
            return providerDetails.getPemCertificateFrom();
        } else {
            return printDetails.getPemCertificate();
        }
    }

    public String getOrgnrPostkasse() {
        if (canReceiveDigitalPost()) {
            return providerDetails.getProviderUrl();
        }
        return printDetails.getPostkasseleverandoerAdresse();
    }

    public String getPostkasseAdresse() {
        return providerDetails.getMailboxId();
    }

    public static KontaktInfo from(HentPersonerRespons hentPersonerRespons) {
        return hentPersonerRespons.getPerson().stream().findFirst().flatMap(PersonKontaktInfoMapper::map).orElseThrow(() -> new KontaktInfoException("Mangler kontaktinformasjon"));
    }

    /**
     * @return the email address to send notifications to
     */
    public String getEpostadresse() {
        return personDetails.getEmailAdress();
    }

    /**
     * @return the mobile phone number to send notifications to
     */
    public String getMobiltelefonnummer() {
        return personDetails.getPhoneNumber();
    }

    /**
     * Status indicates whether or not to notify the recipient of a sent message
     * @return Enum value
     */
    public boolean isNotifiable() {
        return personDetails.isNotifiable();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("providerDetails", providerDetails)
                .add("personDetails", personDetails)
                .toString();
    }

    public boolean isReservert() {
        return personDetails.isReservert();
    }

    public boolean canReceiveDigitalPost() {
        return (providerDetails.hasMailbox()) && !personDetails.isReservert() && personDetails.isAktiv();
    }

    public boolean hasMailbox() {
        return providerDetails.hasMailbox();
    }

    public void setPrintDetails(PrintProviderDetails printDetails) {
        this.printDetails = printDetails;
    }
}

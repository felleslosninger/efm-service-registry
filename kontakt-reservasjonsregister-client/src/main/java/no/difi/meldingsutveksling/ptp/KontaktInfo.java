package no.difi.meldingsutveksling.ptp;

import com.google.common.base.MoreObjects;
import no.difi.ptp.sikkerdigitalpost.HentPersonerRespons;

public class KontaktInfo {
    private final PersonKontaktInfoMapper.MailboxProvider providerDetails;
    private final PersonKontaktInfoMapper.PersonDetails personDetails;

    KontaktInfo(PersonKontaktInfoMapper.MailboxProvider providerDetails, PersonKontaktInfoMapper.PersonDetails personDetails) {
        this.providerDetails = providerDetails;
        this.personDetails = personDetails;
    }

    public String getCertificate() {
        return providerDetails.getPemCertificateFrom();
    }

    public String getOrgnrPostkasse() {
        return providerDetails.getProviderUrl();
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
    public String getVarslingsstatus() {
        return personDetails.getReserved();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("providerDetails", providerDetails)
                .add("personDetails", personDetails)
                .toString();
    }
}

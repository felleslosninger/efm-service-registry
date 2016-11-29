package no.difi.meldingsutveksling.ptp;

import com.google.common.base.MoreObjects;
import no.difi.ptp.sikkerdigitalpost.Person;
import no.difi.ptp.sikkerdigitalpost.Reservasjon;
import no.difi.ptp.sikkerdigitalpost.Status;
import no.difi.ptp.sikkerdigitalpost.Varslingsstatus;

import java.util.Optional;

class PersonKontaktInfoMapper {

    private PersonKontaktInfoMapper() {
    }

    static Optional<KontaktInfo> map(Person person) {
        PersonWrapper pw = new PersonWrapper(person);

        String pemCertificate = "";


        if(pw.hasCertificate()) {
            pemCertificate = CertificateUtil.pemCertificateFrom(pw.getX509Sertifikat());
        }
        MailboxProvider providerDetails = MailboxProvider.EMPTY;
        if(pw.hasMailbox()) {
            providerDetails = new MailboxProvider(pemCertificate,
                    person.getSikkerDigitalPostAdresse().getPostkasseleverandoerAdresse(),
                    person.getSikkerDigitalPostAdresse().getPostkasseadresse()
            );
        }

        PersonDetails personDetails = PersonDetails.EMPTY;
        if (hasKontaktInformasjon(person)) {
            personDetails = new PersonDetails(person.getKontaktinformasjon().getEpostadresse().getValue(),
                    person.getKontaktinformasjon().getMobiltelefonnummer().getValue(),
                    person.getVarslingsstatus() == Varslingsstatus.KAN_VARSLES,
                    person.getReservasjon() == Reservasjon.JA,
                    person.getStatus() == Status.AKTIV);
        }



        return Optional.of(new KontaktInfo(providerDetails, personDetails));
    }

    private static boolean hasKontaktInformasjon(Person person) {
        return person.getKontaktinformasjon() != null;
    }

    static class MailboxProvider {
        public static final MailboxProvider EMPTY = new MailboxProvider(null, null, null);
        private final String pemCertificateFrom;
        private final String leverandoerAdresse;
        private final String postkasseAdresse;

        MailboxProvider(String pemCertificateFrom, String leverandoerAdresse, String postkasseAdresse) {
            this.pemCertificateFrom = pemCertificateFrom;
            this.leverandoerAdresse = leverandoerAdresse;
            this.postkasseAdresse = postkasseAdresse;
        }

        public MailboxProvider() {
            pemCertificateFrom = null;
            leverandoerAdresse = null;
            postkasseAdresse = null;
        }

        String getPemCertificateFrom() {
            return pemCertificateFrom;
        }

        String getProviderUrl() {
            return leverandoerAdresse;
        }

        String getMailboxId() {
            return postkasseAdresse;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("pemCertificateFrom", pemCertificateFrom)
                    .add("leverandoerAdresse", leverandoerAdresse)
                    .add("postkasseAdresse", postkasseAdresse)
                    .toString();
        }

        /**
         *
         * @return true if all necessary fields for sending digital post to a mailbox provider are available
         */
        public boolean hasMailbox() {
            return pemCertificateFrom != null && leverandoerAdresse != null && postkasseAdresse != null;
        }
    }

    static class PersonDetails {
        public static final PersonDetails EMPTY = new PersonDetails(null, null, false, true, false);
        private final String emailAdress;
        private final String phoneNumber;
        private final boolean notifiable;
        private final boolean reservert;
        private boolean aktiv;

        PersonDetails(String emailAdress, String phoneNumber, boolean notifiable, boolean reservert, boolean aktiv) {
            this.emailAdress = emailAdress;
            this.phoneNumber = phoneNumber;
            this.notifiable = notifiable;
            this.reservert = reservert;
            this.aktiv = aktiv;
        }

        String getEmailAdress() {
            return emailAdress;
        }

        String getPhoneNumber() {
            return phoneNumber;
        }

        public boolean isNotifiable() {
            return notifiable;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("emailAdress", emailAdress)
                    .add("phoneNumber", phoneNumber)
                    .add("notifiable", notifiable)
                    .toString();
        }

        public boolean isReservert() {
            return reservert;
        }

        /**
         * @return true if user is defined as active in DPI
         */
        public boolean isAktiv() {
            return aktiv;
        }

        /**
         *
         * @return true if all necessary details are in place for sending digital post
         */
        public boolean isEmpty() {
            return emailAdress == null && phoneNumber == null;
        }
    }
}

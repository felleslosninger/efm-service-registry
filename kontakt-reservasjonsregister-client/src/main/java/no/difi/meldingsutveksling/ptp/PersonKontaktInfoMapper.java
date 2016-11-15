package no.difi.meldingsutveksling.ptp;

import com.google.common.base.MoreObjects;
import no.difi.ptp.sikkerdigitalpost.Person;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Optional;

class PersonKontaktInfoMapper {

    private PersonKontaktInfoMapper() {
    }

    static Optional<KontaktInfo> map(Person person) {
        MailboxProvider providerDetails = new MailboxProvider(pemCertificateFrom(person.getX509Sertifikat()),
                person.getSikkerDigitalPostAdresse().getPostkasseleverandoerAdresse(),
                person.getSikkerDigitalPostAdresse().getPostkasseadresse()
                );

        PersonDetails personDetails = new PersonDetails(person.getKontaktinformasjon().getEpostadresse().getValue(),
                person.getKontaktinformasjon().getMobiltelefonnummer().getValue(),
                person.getVarslingsstatus().value());

        return Optional.of(new KontaktInfo(providerDetails, personDetails));
    }

    private static String pemCertificateFrom(byte[] certificateBytes) {
        final StringWriter sw = new StringWriter();
        try(JcaPEMWriter pemWriter = new JcaPEMWriter(sw)) {
            CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
            final X509Certificate certificate = (X509Certificate) certFactory.generateCertificate(new ByteArrayInputStream(certificateBytes));
            pemWriter.writeObject(certificate);
            pemWriter.flush();
            return sw.toString();
        } catch (IOException |CertificateException e) {
            throw new RuntimeException("Failed to create pem certificate from bytes", e);
        }
    }

    static class MailboxProvider {
        private final String pemCertificateFrom;
        private final String leverandoerAdresse;
        private final String postkasseAdresse;

        MailboxProvider(String pemCertificateFrom, String leverandoerAdresse, String postkasseAdresse) {
            this.pemCertificateFrom = pemCertificateFrom;
            this.leverandoerAdresse = leverandoerAdresse;
            this.postkasseAdresse = postkasseAdresse;
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
    }

    static class PersonDetails {
        private final String emailAdress;
        private final String phoneNumber;
        private final String reserved;

        PersonDetails(String emailAdress, String phoneNumber, String reserved) {
            this.emailAdress = emailAdress;
            this.phoneNumber = phoneNumber;
            this.reserved = reserved;
        }

        String getEmailAdress() {
            return emailAdress;
        }

        String getPhoneNumber() {
            return phoneNumber;
        }

        public String getReserved() {
            return reserved;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("emailAdress", emailAdress)
                    .add("phoneNumber", phoneNumber)
                    .add("reserved", reserved)
                    .toString();
        }
    }
}

package no.difi.meldingsutveksling.ptp;

import no.difi.ptp.sikkerdigitalpost.Person;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Optional;

public class PersonKontaktInfoMapper {
    static Optional<KontaktInfo> map(Person person) {
        return Optional.of(new KontaktInfo(pemCertificateFrom(person.getX509Sertifikat()), person.getSikkerDigitalPostAdresse().getPostkasseleverandoerAdresse(), person.getSikkerDigitalPostAdresse().getPostkasseadresse(), person.getKontaktinformasjon().getEpostadresse().getValue(), person.getKontaktinformasjon().getMobiltelefonnummer().getValue(), person.getVarslingsstatus()));
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
}

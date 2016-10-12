package no.difi.meldingsutveksling.ptp;

import com.google.common.base.MoreObjects;
import no.difi.ptp.sikkerdigitalpost.HentPersonerRespons;
import no.difi.ptp.sikkerdigitalpost.Person;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.util.io.pem.PemWriter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Optional;
import java.util.function.Function;

public class KontaktInfo {
    private static Function<? super Person, Optional<KontaktInfo>> personMapperKontaktInfo = (Function<Person, Optional<KontaktInfo>>) person -> Optional.of(new KontaktInfo(pemCertificateFrom(person.getX509Sertifikat()), person.getSikkerDigitalPostAdresse().getPostkasseleverandoerAdresse(), person.getSikkerDigitalPostAdresse().getPostkasseadresse()));
    String certificate;
    String orgnrPostkasse;
    String postkasseAdresse;

    public KontaktInfo(String certificate, String orgnrPostkasse, String postkasseAdresse) {
        this.certificate = certificate;
        this.orgnrPostkasse = orgnrPostkasse;
        this.postkasseAdresse = postkasseAdresse;
    }

    public String getCertificate() {
        return certificate;
    }

    public String getOrgnrPostkasse() {
        return orgnrPostkasse;
    }

    public String getPostkasseAdresse() {
        return postkasseAdresse;
    }

    public static KontaktInfo from(HentPersonerRespons hentPersonerRespons) {
        return hentPersonerRespons.getPerson().stream().findFirst().flatMap(personMapperKontaktInfo).orElseThrow(() -> new KontaktInfoException("Mangler kontaktinformasjon"));
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("certificate", certificate)
                .add("orgnrPostkasse", orgnrPostkasse)
                .add("postkasseAdresse", postkasseAdresse)
                .toString();
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

package no.difi.meldingsutveksling.ptp;

import com.google.common.base.MoreObjects;
import no.difi.ptp.sikkerdigitalpost.*;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

public class KontaktInfo {
    private final String certificate;
    private final String orgnrPostkasse;
    private final String postkasseAdresse;
    private final String epostadresse;
    private final String mobiltelefonnummer;
    private final String varslingsstatus;

    public KontaktInfo(String certificate, String orgnrPostkasse, String postkasseAdresse, String epostadresse, String mobiltelefonnummer, String varslingsstatus) {
        this.certificate = certificate;
        this.orgnrPostkasse = orgnrPostkasse;
        this.postkasseAdresse = postkasseAdresse;
        this.epostadresse = epostadresse;
        this.mobiltelefonnummer = mobiltelefonnummer;
        this.varslingsstatus = varslingsstatus;
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
        return hentPersonerRespons.getPerson().stream().findFirst().flatMap(PersonKontaktInfoMapper::map).orElseThrow(() -> new KontaktInfoException("Mangler kontaktinformasjon"));
    }

    /**
     * @return the email address to send notifications to
     */
    public String getEpostadresse() {
        return epostadresse;
    }

    /**
     * @return the mobile phone number to send notifications to
     */
    public String getMobiltelefonnummer() {
        return mobiltelefonnummer;
    }

    /**
     * Status indicates whether or not to notify the recipient of a sent message
     * @return Enum value
     */
    public String getVarslingsstatus() {
        return varslingsstatus;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("certificate", certificate)
                .add("orgnrPostkasse", orgnrPostkasse)
                .add("postkasseAdresse", postkasseAdresse)
                .add("epostadresse", epostadresse)
                .add("mobiltelefonnummer", mobiltelefonnummer)
                .add("varslingsstatus", varslingsstatus)
                .toString();
    }
}

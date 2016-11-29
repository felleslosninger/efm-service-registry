package no.difi.meldingsutveksling.ptp;

import org.bouncycastle.openssl.jcajce.JcaPEMWriter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

class Util {
    private Util() {
    }

    static String pemCertificateFrom(byte[] certificateBytes) {
        if (certificateBytes == null) {
            throw new PemCertificateException("Input certificate cannot be null");
        }
        final StringWriter sw = new StringWriter();
        try(JcaPEMWriter pemWriter = new JcaPEMWriter(sw)) {
            CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
            final X509Certificate certificate = (X509Certificate) certFactory.generateCertificate(new ByteArrayInputStream(certificateBytes));
            pemWriter.writeObject(certificate);
            pemWriter.flush();
            return sw.toString();
        } catch (IOException |CertificateException e) {
            throw new PemCertificateException("Failed to create pem certificate from bytes", e);
        }
    }

    private static class PemCertificateException extends RuntimeException {
        PemCertificateException(String s) {
            super(s);
        }

        PemCertificateException(String s, Exception e) {
            super(s, e);
        }
    }
}

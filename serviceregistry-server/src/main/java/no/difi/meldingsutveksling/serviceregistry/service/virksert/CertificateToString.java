package no.difi.meldingsutveksling.serviceregistry.service.virksert;

import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryException;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;

import java.io.IOException;
import java.io.StringWriter;
import java.security.cert.Certificate;

public class CertificateToString {

    public static String toString(Certificate certificate) {
        StringWriter stringWriter = new StringWriter();
        JcaPEMWriter jcaPEMWriter = new JcaPEMWriter(stringWriter);
        try {
            jcaPEMWriter = new JcaPEMWriter(stringWriter);
            jcaPEMWriter.writeObject(certificate);
            jcaPEMWriter.flush();
            return stringWriter.toString();
        } catch (IOException e) {
            throw new ServiceRegistryException(e);
        } finally {
            IOUtils.closeQuietly(jcaPEMWriter);
            IOUtils.closeQuietly(stringWriter);
        }
    }
}
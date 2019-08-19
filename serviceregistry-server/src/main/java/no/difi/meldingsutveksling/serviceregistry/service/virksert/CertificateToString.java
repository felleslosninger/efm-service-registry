package no.difi.meldingsutveksling.serviceregistry.service.virksert;

import lombok.experimental.UtilityClass;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryException;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;

import java.io.IOException;
import java.io.StringWriter;
import java.security.cert.Certificate;

@UtilityClass
public class CertificateToString {

    public static String toString(Certificate certificate) {


        try (
                StringWriter stringWriter = new StringWriter();
                JcaPEMWriter jcaPEMWriter = new JcaPEMWriter(stringWriter);
        ) {
            jcaPEMWriter.writeObject(certificate);
            jcaPEMWriter.flush();
            return stringWriter.toString();
        } catch (IOException e) {
            throw new ServiceRegistryException(e);
        }
    }
}
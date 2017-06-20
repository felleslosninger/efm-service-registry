package no.difi.meldingsutveksling.serviceregistry.security;

import com.google.common.collect.Lists;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.util.Base64;
import no.difi.move.common.oauth.KeystoreHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.security.cert.CertificateEncodingException;
import java.util.List;

@Component
public class PayloadSigner {

    private static final Logger log = LoggerFactory.getLogger(PayloadSigner.class);

    private KeystoreHelper keystoreHelper;

    @Autowired
    public PayloadSigner(KeystoreHelper keystoreHelper) {
        this.keystoreHelper = keystoreHelper;
    }

    public String sign(String input) throws EntitySignerException {

        Payload payload = new Payload(input);

        List<Base64> certChain = Lists.newArrayList();
        try {
            certChain.add(Base64.encode(keystoreHelper.getX509Certificate().getEncoded()));
        } catch (CertificateEncodingException e) {
            log.error("Failed fetching encoded certificate from keystore.", e);
            throw new EntitySignerException(e);
        }

        JWSHeader jwsHeader = new JWSHeader.Builder(JWSAlgorithm.RS256).x509CertChain(certChain).build();
        JWSObject jwsObject = new JWSObject(jwsHeader, payload);
        try {
            jwsObject.sign(new RSASSASigner(keystoreHelper.loadPrivateKey()));
        } catch (JOSEException e) {
            log.error("Failed to sign JWS object.", e);
            throw new EntitySignerException(e);
        }

        return jwsObject.serialize();
    }
}

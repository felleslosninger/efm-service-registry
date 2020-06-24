package no.difi.meldingsutveksling.serviceregistry.security;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import lombok.RequiredArgsConstructor;
import no.difi.move.common.oauth.KeystoreHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PayloadSigner {

    private static final Logger log = LoggerFactory.getLogger(PayloadSigner.class);

    private final KeystoreHelper keystoreHelper;
    private final RSAKey rsaKey;

    public String sign(String input) throws EntitySignerException {
        Payload payload = new Payload(input);
        JWSHeader jwsHeader = new JWSHeader.Builder(JWSAlgorithm.RS256)
                .x509CertChain(rsaKey.getX509CertChain())
                .keyID(rsaKey.getKeyID())
                .build();
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

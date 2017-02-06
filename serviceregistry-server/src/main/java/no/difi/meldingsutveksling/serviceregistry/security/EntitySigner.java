package no.difi.meldingsutveksling.serviceregistry.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.util.Base64;
import no.difi.meldingsutveksling.serviceregistry.controller.EntityResource;
import no.difi.move.common.oauth.KeystoreHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.security.cert.CertificateEncodingException;
import java.util.List;

@Component
public class EntitySigner {

    private static final Logger log = LoggerFactory.getLogger(EntitySigner.class);

    @Autowired
    private KeystoreHelper keystoreHelper;

    public String sign(EntityResource entity) throws EntitySignerException {

        String json = null;
        try {
            json = new ObjectMapper().writeValueAsString(entity);
        } catch (JsonProcessingException e) {
            log.error("Failed to convert entity to json", e);
            throw new EntitySignerException(e);
        }
        Payload payload = new Payload(json);

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

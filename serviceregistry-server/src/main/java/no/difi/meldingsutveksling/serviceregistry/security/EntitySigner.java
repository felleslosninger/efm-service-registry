package no.difi.meldingsutveksling.serviceregistry.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.util.Base64;
import no.difi.meldingsutveksling.serviceregistry.controller.EntityResource;
import no.difi.move.common.oauth.KeystoreHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.security.cert.CertificateEncodingException;
import java.util.List;

@Component
public class EntitySigner {

    @Autowired
    private KeystoreHelper keystoreHelper;

    public String sign(EntityResource entity) throws JsonProcessingException, CertificateEncodingException, JOSEException {

        String json = new ObjectMapper().writeValueAsString(entity);
        Payload payload = new Payload(json);

        List<Base64> certChain = Lists.newArrayList();
        certChain.add(Base64.encode(keystoreHelper.getX509Certificate().getEncoded()));
        JWSHeader jwsHeader = new JWSHeader.Builder(JWSAlgorithm.RS256).x509CertChain(certChain).build();
        JWSObject jwsObject = new JWSObject(jwsHeader, payload);
        jwsObject.sign(new RSASSASigner(keystoreHelper.loadPrivateKey()));

        return jwsObject.serialize();
    }
}

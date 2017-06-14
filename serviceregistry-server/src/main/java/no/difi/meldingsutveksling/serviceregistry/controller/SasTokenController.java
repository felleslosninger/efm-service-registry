package no.difi.meldingsutveksling.serviceregistry.controller;

import com.google.common.base.Strings;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.meldingsutveksling.serviceregistry.config.ServiceregistryProperties;
import no.difi.meldingsutveksling.serviceregistry.security.EntitySignerException;
import no.difi.meldingsutveksling.serviceregistry.security.PayloadSigner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@RestController
public class SasTokenController {

    private ServiceregistryProperties props;
    private PayloadSigner payloadSigner;

    @Autowired
    SasTokenController(ServiceregistryProperties props,
                       PayloadSigner payloadSigner) {
        this.props = props;
        this.payloadSigner = payloadSigner;
    }

    @PreAuthorize("#oauth2.hasScope('move/dpe.read')")
    @RequestMapping(value = "/sastoken", method = RequestMethod.GET, produces = "application/jose")
    public ResponseEntity getToken(Authentication auth) throws EntitySignerException {

        if (auth == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Audit.info(String.format("SAS token request by %s", (String) auth.getPrincipal()));

        String sasToken = props.getAuth().getSasToken();
        if (Strings.isNullOrEmpty(sasToken)) {
            Audit.error("SAS token not defined");
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(payloadSigner.sign(sasToken));
    }

    private String genSASToken(String resourceUri, String keyName, String key) {

        long epoch = System.currentTimeMillis() / 1000L;
        String expirySeconds = Long.toString(epoch + 60 * 3);

        String sasToken = null;
        try {
            String stringToSign = String.format("%s\n%s", URLEncoder.encode(resourceUri, "UTF-8"), expirySeconds);
            String signature = getHMAC256(key, stringToSign);
            sasToken = String.format("SharedAccessSignature sr=%s&sig=%s&se=%s&skn%s",
                    URLEncoder.encode(resourceUri, "UTF-8"),
                    URLEncoder.encode(signature, "UTF-8"),
                    expirySeconds, keyName);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return sasToken;
    }

    private String getHMAC256(String key, String input) {
        Mac sha256_HMAC = null;
        String hash = null;
        try {
            sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = new SecretKeySpec(key.getBytes(), "HmacSHA256");
            sha256_HMAC.init(secret_key);
            Base64.Encoder encoder = Base64.getEncoder();

            hash = new String(encoder.encode(sha256_HMAC.doFinal(input.getBytes("UTF-8"))));

        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return hash;
    }
}

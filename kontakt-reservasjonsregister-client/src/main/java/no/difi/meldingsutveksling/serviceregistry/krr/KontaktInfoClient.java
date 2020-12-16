package no.difi.meldingsutveksling.serviceregistry.krr;

import com.nimbusds.jose.proc.BadJWSException;
import no.difi.move.common.oauth.JWTDecoder;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.security.cert.CertificateException;
import java.util.Objects;

abstract class KontaktInfoClient {

    String fetchKontaktInfo(String identifier, String token, URI uri) throws KontaktInfoException {
        PersonRequest request = PersonRequest.of(identifier);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        headers.set("Accept", "application/jose");
        HttpEntity<Object> httpEntity = new HttpEntity<>(request, headers);

        RestTemplate rt = new RestTemplate();
        ResponseEntity<String> response = rt.exchange(uri, HttpMethod.POST, httpEntity, String.class);

        if (response.getStatusCode() != HttpStatus.OK) {
            throw new KontaktInfoException(String.format("%s endpoint returned %s (%s)", uri, response.getStatusCode().value(),
                    response.getStatusCode().getReasonPhrase()));
        }

        String payload;
        try {
            JWTDecoder jwtDecoder = new JWTDecoder();
            payload = jwtDecoder.getPayload(Objects.requireNonNull(response.getBody()));
        } catch (CertificateException | BadJWSException e) {
            throw new KontaktInfoException(String.format("Error during decoding JWT response from %s", uri), e);
        }

        return payload;
    }

}

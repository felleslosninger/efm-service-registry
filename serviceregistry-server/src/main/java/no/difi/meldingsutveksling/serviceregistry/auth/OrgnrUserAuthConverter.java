package no.difi.meldingsutveksling.serviceregistry.auth;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.provider.token.DefaultUserAuthenticationConverter;

import java.util.Map;

public class OrgnrUserAuthConverter extends DefaultUserAuthenticationConverter {

    private static final String CLIENT_ORGNR = "client_orgno";

    @Override
    public Authentication extractAuthentication(Map<String, ?> map) {
        if (map.containsKey(CLIENT_ORGNR)) {
            Object principal = map.get(CLIENT_ORGNR);
            return new UsernamePasswordAuthenticationToken(principal, "N/A", null);
        }
        return null;
    }
}

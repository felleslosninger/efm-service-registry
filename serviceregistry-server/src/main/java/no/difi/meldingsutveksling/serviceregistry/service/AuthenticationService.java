package no.difi.meldingsutveksling.serviceregistry.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;

import static no.difi.meldingsutveksling.serviceregistry.logging.SRMarkerFactory.markerFrom;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private static final String CLAIM_CLIENT_ORGNO = "client_orgno";

    public String getToken(Authentication auth) {
        JwtAuthenticationToken jwtToken = (JwtAuthenticationToken) auth;
        return jwtToken.getToken().getTokenValue();
    }

    public String getAuthorizedClientIdentifier(Authentication auth, HttpServletRequest request) {
        JwtAuthenticationToken token = (JwtAuthenticationToken) auth;
        String clientOrgnr = token.getToken().getClaimAsString(CLAIM_CLIENT_ORGNO);
        if (clientOrgnr != null) {
            log.debug(String.format("Authorized lookup request by %s", clientOrgnr),
                    markerFrom(request.getRemoteAddr(), request.getRemoteHost(), clientOrgnr));
        } else {
            log.debug(String.format("Unauthorized lookup request from %s", request.getRemoteAddr()),
                    markerFrom(request.getRemoteAddr()));
        }
        return clientOrgnr;
    }

}

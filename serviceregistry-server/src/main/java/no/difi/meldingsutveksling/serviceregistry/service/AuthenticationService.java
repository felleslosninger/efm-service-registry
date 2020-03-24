package no.difi.meldingsutveksling.serviceregistry.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetails;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;

import static no.difi.meldingsutveksling.serviceregistry.logging.SRMarkerFactory.markerFrom;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final TokenStore tokenStore;

    public String getToken(Authentication auth) {
        OAuth2AuthenticationDetails details = (OAuth2AuthenticationDetails) auth.getDetails();
        return details.getTokenValue();
    }

    public String getAuthorizedClientIdentifier(Authentication auth, HttpServletRequest request) {
        OAuth2AccessToken oAuth2AccessToken = tokenStore.readAccessToken(getToken(auth));
        String clientOrgnr = (String) oAuth2AccessToken.getAdditionalInformation().get("client_orgno");
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

package no.difi.meldingsutveksling.serviceregistry.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;

import static no.difi.meldingsutveksling.serviceregistry.logging.SRMarkerFactory.markerFrom;

@Slf4j
@Service
public class AuthenticationService {

    public String getOrganizationNumber(Authentication auth, HttpServletRequest request) {
        String clientOrgnr = auth == null ? null : (String) auth.getPrincipal();
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

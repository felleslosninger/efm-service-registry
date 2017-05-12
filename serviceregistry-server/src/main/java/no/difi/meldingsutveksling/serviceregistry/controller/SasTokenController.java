package no.difi.meldingsutveksling.serviceregistry.controller;

import com.google.common.base.Strings;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.meldingsutveksling.serviceregistry.config.ServiceregistryProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SasTokenController {

    private ServiceregistryProperties props;

    @Autowired
    SasTokenController(ServiceregistryProperties props) {
        this.props = props;
    }

    @RequestMapping(value = "/sastoken", method = RequestMethod.GET, produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity getToken(Authentication auth) {

        if (auth == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String clientOrgnr = (String) auth.getPrincipal();
        Audit.info(String.format("SAS token request by %s", clientOrgnr));

        String sasToken = props.getAuth().getSasToken();
        if (Strings.isNullOrEmpty(sasToken)) {
            Audit.info("SAS token not defined, returning 404");
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(sasToken);
    }

}

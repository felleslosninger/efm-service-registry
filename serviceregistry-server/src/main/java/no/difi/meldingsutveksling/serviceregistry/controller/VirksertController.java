package no.difi.meldingsutveksling.serviceregistry.controller;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.serviceregistry.CertificateNotFoundException;
import no.difi.meldingsutveksling.serviceregistry.domain.ServiceIdentifier;
import no.difi.meldingsutveksling.serviceregistry.exceptions.NonMatchingCertificatesException;
import no.difi.meldingsutveksling.serviceregistry.security.EntitySignerException;
import no.difi.meldingsutveksling.serviceregistry.security.PayloadSigner;
import no.difi.meldingsutveksling.serviceregistry.service.AuthenticationService;
import no.difi.meldingsutveksling.serviceregistry.service.virksert.VirkSertService;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/virksert")
@RequiredArgsConstructor
public class VirksertController {

    private final VirkSertService virkSertService;
    private final PayloadSigner payloadSigner;
    private final AuthenticationService authenticationService;

    @GetMapping(value = "/{identifier}", produces = MediaType.TEXT_PLAIN_VALUE)
    public String getCertificate(@PathVariable String identifier, Authentication auth) throws CertificateNotFoundException {
        String scope = (String) authenticationService.getToken(auth).getClaims().get("scope");
        if (containsDpoScope(scope) && containsDpeScope(scope)) {
            String dpoCert = virkSertService.getCertificate(identifier, ServiceIdentifier.DPO);
            String dpeCert = virkSertService.getCertificate(identifier, ServiceIdentifier.DPE);
            if (!dpoCert.equals(dpeCert)) {
                throw new NonMatchingCertificatesException(identifier);
            }
            return dpoCert;
        } else if (containsDpoScope(scope)) {
            return virkSertService.getCertificate(identifier, ServiceIdentifier.DPO);
        } else if (containsDpeScope(scope)) {
            return virkSertService.getCertificate(identifier, ServiceIdentifier.DPE);
        } else {
            return virkSertService.getCertificate(identifier, ServiceIdentifier.DPO);
        }
    }

    @GetMapping(value = "/{identifier}", produces = "application/jose")
    public String getCertificateJose(@PathVariable String identifier, Authentication auth) throws CertificateNotFoundException, EntitySignerException {
        return payloadSigner.sign(getCertificate(identifier, auth));
    }

    private boolean containsDpoScope(String scope){
        return (scope.contains("move/dpo.read") || scope.contains("eformidling:dpo"));
    }

    private boolean containsDpeScope(String scope){
        return (scope.contains("move/dpe.read") || scope.contains("eformidling:dpe"));
    }
}
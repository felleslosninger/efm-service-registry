package no.difi.meldingsutveksling.serviceregistry.service.virksert;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.serviceregistry.CertificateNotFoundException;
import no.difi.meldingsutveksling.serviceregistry.config.ServiceregistryProperties;
import no.difi.meldingsutveksling.serviceregistry.domain.ServiceIdentifier;
import no.difi.meldingsutveksling.serviceregistry.exceptions.ServiceRegistryException;
import no.difi.vefa.peppol.common.lang.PeppolParsingException;
import no.difi.vefa.peppol.common.model.ParticipantIdentifier;
import no.difi.vefa.peppol.common.model.ProcessIdentifier;
import no.difi.virksert.api.Mode;
import no.difi.virksert.client.BusinessCertificateClient;
import no.difi.virksert.client.lang.VirksertClientException;
import no.difi.virksert.lang.BusinessCertificateException;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.cert.X509Certificate;

@Component
@RequiredArgsConstructor
public class VirkSertService {

    private final ServiceregistryProperties properties;
    private final Environment env;
    private BusinessCertificateClient virksertClient;

    @PostConstruct
    public void init() {

        URI virksertUrl;
        try {
            virksertUrl = properties.getVirksert().getEndpointURL().toURI();
        } catch (URISyntaxException e) {
            throw new ServiceRegistryException(e);
        }

        try {
            if (env.acceptsProfiles(Profiles.of("production"))) {
                virksertClient = BusinessCertificateClient.of(virksertUrl, Mode.PRODUCTION);
            } else {
                virksertClient = BusinessCertificateClient.of(virksertUrl, Mode.MOVE);
            }
        } catch (BusinessCertificateException e) {
            throw new ServiceRegistryException(e);
        }
    }

    public String getCertificate(String orgnr, ServiceIdentifier si) throws CertificateNotFoundException {
        if (!properties.getVirksert().getProcesses().containsKey(si)) {
            throw new IllegalArgumentException("Virksert process not registered for service identifier: "+si);
        }

        ProcessIdentifier dpoProcess;
        try {
            dpoProcess = ProcessIdentifier.parse(properties.getVirksert().getProcesses().get(si));
        } catch (PeppolParsingException e) {
            throw new IllegalArgumentException(e);
        }

        try {
            X509Certificate cert = virksertClient.fetchCertificate(ParticipantIdentifier.of(properties.getVirksert().getIcd() + ":" + orgnr), dpoProcess);
            return CertificateToString.toString(cert);
        } catch (VirksertClientException e) {
            throw new CertificateNotFoundException(String.format("Unable to find %s certificate for: %s", si.name(), orgnr), e);
        }
    }

}

package no.difi.meldingsutveksling.serviceregistry.service.virksert;

import lombok.RequiredArgsConstructor;
import network.oxalis.vefa.peppol.common.lang.PeppolParsingException;
import network.oxalis.vefa.peppol.common.model.ParticipantIdentifier;
import network.oxalis.vefa.peppol.common.model.ProcessIdentifier;
import no.difi.meldingsutveksling.serviceregistry.CertificateNotFoundException;
import no.difi.meldingsutveksling.serviceregistry.config.ServiceregistryProperties;
import no.difi.meldingsutveksling.serviceregistry.domain.ServiceIdentifier;
import no.difi.virksert.client.BusinessCertificateClient;
import no.difi.virksert.client.lang.VirksertClientException;
import org.springframework.stereotype.Component;

import java.security.cert.X509Certificate;

@Component
@RequiredArgsConstructor
public class VirkSertService {

    private final ServiceregistryProperties properties;
    private final BusinessCertificateClient virksertClient;

    public String getCertificate(String orgnr, ServiceIdentifier si) throws CertificateNotFoundException {
        if (!properties.getVirksert().getProcesses().containsKey(si)) {
            throw new IllegalArgumentException("Virksert process not registered for service identifier: " + si);
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

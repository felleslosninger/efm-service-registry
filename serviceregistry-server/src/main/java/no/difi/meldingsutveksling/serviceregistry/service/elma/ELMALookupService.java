package no.difi.meldingsutveksling.serviceregistry.service.elma;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.domain.Iso6523;
import no.difi.meldingsutveksling.serviceregistry.exceptions.ServiceRegistryException;
import no.difi.vefa.peppol.common.model.*;
import no.difi.vefa.peppol.lookup.LookupClient;
import no.difi.vefa.peppol.lookup.api.LookupException;
import no.difi.vefa.peppol.security.lang.PeppolSecurityException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * See configuraion beans for beans that might be injected as LookupClient and TransportProfile
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ELMALookupService {

    private final LookupClient lookupClient;

    public Set<ProcessIdentifier> lookupRegisteredProcesses(Iso6523 identifier, Set<String> documentIdentifiers) {
        List<ServiceMetadata> smdList = lookup(identifier, documentIdentifiers);
        return smdList.stream()
                .flatMap(smd -> smd.getProcesses().stream())
                .map(ProcessMetadata::getProcessIdentifier)
                .collect(Collectors.toSet());
    }

    public List<ServiceMetadata> lookup(Iso6523 identifier, Set<String> documentIdentifiers) {
        List<ServiceMetadata> metadataList = new ArrayList<>();
        for (String id : documentIdentifiers) {
            try {
                ServiceMetadata serviceMetadata = lookupClient.getServiceMetadata(ParticipantIdentifier.of(identifier.toString()), DocumentTypeIdentifier.of(id));
                if (serviceMetadata != null) {
                    metadataList.add(serviceMetadata);
                }

            } catch (PeppolSecurityException e) {
                throw new ServiceRegistryException(e);
            } catch (LookupException e) {
                // Just log, need to check the remaining documents
                log.debug("Failed ELMA lookup for identifier={}, documentTypeIdentifier={}", identifier, id, e);
            }
        }
        return metadataList;
    }

}
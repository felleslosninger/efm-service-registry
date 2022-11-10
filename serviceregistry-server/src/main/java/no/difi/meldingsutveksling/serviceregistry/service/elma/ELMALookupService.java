package no.difi.meldingsutveksling.serviceregistry.service.elma;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.serviceregistry.exceptions.ServiceRegistryException;
import network.oxalis.vefa.peppol.common.model.*;
import network.oxalis.vefa.peppol.lookup.LookupClient;
import network.oxalis.vefa.peppol.lookup.api.LookupException;
import network.oxalis.vefa.peppol.security.lang.PeppolSecurityException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
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

    public Set<ProcessIdentifier> lookupRegisteredProcesses(String orgnr, Set<String> documentIdentifiers) {
        List<ServiceMetadata> smdList = lookup(orgnr, documentIdentifiers);
            return new HashSet<>((smdList.stream()
//                .flatMap(smd -> smd.getProcesses().stream())
                    .flatMap(smd -> smd.getServiceInformation().getProcesses().stream())
                    .map(ProcessMetadata::getProcessIdentifier).collect(Collectors.toList())).get(0));


    }

    public List<ServiceMetadata> lookup(String organizationNumber, Set<String> documentIdentifiers) {
        List<ServiceMetadata> metadataList = new ArrayList<>();
        for (String id : documentIdentifiers) {
            try {
                ServiceMetadata serviceMetadata = lookupClient.getServiceMetadata(ParticipantIdentifier.of(organizationNumber), DocumentTypeIdentifier.of(id));
                if (serviceMetadata != null) {
                    metadataList.add(serviceMetadata);
                }

            } catch (PeppolSecurityException e) {
                throw new ServiceRegistryException(e);
            } catch (LookupException e) {
                // Just log, need to check the remaining documents
                log.debug("Failed ELMA lookup for identifier={}, documentTypeIdentifier={}", organizationNumber, id, e);
            }
        }
        return metadataList;
    }

}
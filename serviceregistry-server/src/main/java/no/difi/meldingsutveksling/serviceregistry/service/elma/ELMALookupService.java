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
        return smdList.stream()
                .flatMap(smd -> smd.getServiceInformation().getProcesses().stream())
//                    TODO: verify that the list of process identifier only has one object. No documentation on this behavior has been found in Peppol docs. Potential bug alert!
                .map(pmd -> pmd.getProcessIdentifier().get(0))
                .collect(Collectors.toSet());
    }

    public List<ServiceMetadata> lookup(String organizationNumber, Set<String> documentIdentifiers) {
        List<ServiceMetadata> metadataList = new ArrayList<>();
        String logId = "getDocumentIdentifiers";
        try {
            Set<String> registeredIdentifiers = lookupClient.getDocumentIdentifiers(ParticipantIdentifier.of(organizationNumber))
                    .stream().map(DocumentTypeIdentifier::getIdentifier)
                    .collect(Collectors.toSet());
            for (String id : documentIdentifiers) {
                logId = id;
                if (registeredIdentifiers.contains(id)) {
                    ServiceMetadata serviceMetadata = lookupClient.getServiceMetadata(ParticipantIdentifier.of(organizationNumber), DocumentTypeIdentifier.of(id));
                    if (serviceMetadata != null) {
                        metadataList.add(serviceMetadata);
                    }
                } else {
                    log.debug("Skipping ELMA lookup for non-registered document type: identifier={}, documentTypeIdentifier={}", organizationNumber, id);
                }
            }
        } catch (PeppolSecurityException e) {
            throw new ServiceRegistryException(e);
        } catch (LookupException e) {
            // Just log, need to check the remaining documents
            log.debug("Failed ELMA lookup for identifier={}, documentTypeIdentifier={}", organizationNumber, logId, e);
        }
        catch (NullPointerException e) {
            log.debug("Organization {} is not in ELMA", organizationNumber, e);
        }
        return metadataList;
    }

}
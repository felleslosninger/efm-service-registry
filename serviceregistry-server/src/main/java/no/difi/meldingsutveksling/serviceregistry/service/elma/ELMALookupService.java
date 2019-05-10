package no.difi.meldingsutveksling.serviceregistry.service.elma;

import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryException;
import no.difi.meldingsutveksling.serviceregistry.config.ServiceregistryProperties;
import no.difi.meldingsutveksling.serviceregistry.exceptions.EndpointUrlNotFound;
import no.difi.vefa.peppol.common.lang.EndpointNotFoundException;
import no.difi.vefa.peppol.common.model.*;
import no.difi.vefa.peppol.lookup.LookupClient;
import no.difi.vefa.peppol.lookup.api.LookupException;
import no.difi.vefa.peppol.security.lang.PeppolSecurityException;
import org.springframework.beans.factory.annotation.Autowired;
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
public class ELMALookupService {

    @Autowired
    private ServiceregistryProperties props;

    private LookupClient lookupClient;
    private TransportProfile transportProfile;

    @Autowired
    public ELMALookupService(LookupClient lookupClient, TransportProfile transportProfile) {
        this.lookupClient = lookupClient;
        this.transportProfile = transportProfile;
    }

    public Endpoint lookup(String organisationNumber) throws EndpointUrlNotFound {
        try {
            return lookupClient.getEndpoint(ParticipantIdentifier.of(organisationNumber),
                    DocumentTypeIdentifier.of(props.getElma().getDocumentTypeIdentifier()),
                    ProcessIdentifier.of(props.getElma().getProcessIdentifier()),
                    transportProfile);
        } catch (PeppolSecurityException e) {
            throw new ServiceRegistryException(e);
        } catch (LookupException | EndpointNotFoundException e) {
            throw new EndpointUrlNotFound(String.format("Failed lookup %s through ELMA ", organisationNumber), e);
        }
    }

    public Set<ProcessIdentifier> lookupRegisteredProcesses(String orgnr, Set<String> documentIdentifiers) {
        List<ServiceMetadata> smdList = lookup(orgnr, documentIdentifiers);
        return smdList.stream()
                .flatMap(smd -> smd.getProcesses().stream())
                .map(ProcessMetadata::getProcessIdentifier)
                .collect(Collectors.toSet());
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

    public Endpoint lookup(String organisationNumber, String documentTypeIdentifier, String processIdentifier) throws EndpointUrlNotFound {
        try {
            return lookupClient.getEndpoint(ParticipantIdentifier.of(organisationNumber),
                    DocumentTypeIdentifier.of(documentTypeIdentifier),
                    ProcessIdentifier.of(processIdentifier),
                    transportProfile);
        } catch (PeppolSecurityException e) {
            throw new ServiceRegistryException(e);
        } catch (LookupException | EndpointNotFoundException e) {
            throw new EndpointUrlNotFound(String.format("Failed lookup through ELMA of %s with document type %s and process %s.", organisationNumber, documentTypeIdentifier, processIdentifier), e);
        }
    }

}
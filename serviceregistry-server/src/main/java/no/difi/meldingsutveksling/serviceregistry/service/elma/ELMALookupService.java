package no.difi.meldingsutveksling.serviceregistry.service.elma;

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

import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * See configuraion beans for beans that might be injected as LookupClient and TransportProfile
 */
@Component
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

    public List<ServiceMetadata> lookup(String organizationNumber, List<String> documentIdentifiers) throws EndpointUrlNotFound {
        try {
            List<ServiceMetadata> metadataList = new ArrayList<>();

            for (String id : documentIdentifiers) {
                ServiceMetadata serviceMetadata = lookupClient.getServiceMetadata(ParticipantIdentifier.of(organizationNumber), DocumentTypeIdentifier.of(id));
                if (serviceMetadata != null) {
                    metadataList.add(serviceMetadata);
                }
            }
            return metadataList;
        } catch (PeppolSecurityException e) {
            throw new ServiceRegistryException(e);
        } catch (LookupException e) {
            throw new EndpointUrlNotFound(String.format("Failed lookup %s through ELMA ", organizationNumber), e);
        }
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
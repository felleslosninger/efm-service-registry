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
            throw new EndpointUrlNotFound(String.format("Failed lookup through ELMA of %s", organisationNumber), e);
        }
    }

    public boolean identifierHasInnsynskravCapability(String identifier) {
        return identifierHasCapability(identifier, props.getElmaDPEInnsyn());
    }

    public boolean identifierHasInnsynDataCapability(String identifier) {
        return identifierHasCapability(identifier, props.getElmaDPEData());
    }

    private boolean identifierHasCapability(String identifier, ServiceregistryProperties.ELMA elmaProp) {
        try {
            Endpoint ep = lookupClient.getEndpoint(ParticipantIdentifier.of(identifier),
                    DocumentTypeIdentifier.of(elmaProp.getDocumentTypeIdentifier()),
                    ProcessIdentifier.of(elmaProp.getProcessIdentifier()),
                    transportProfile);
            return !isNullOrEmpty(ep.getAddress().toString());
        } catch (PeppolSecurityException e) {
            throw new ServiceRegistryException(e);
        } catch (LookupException | EndpointNotFoundException e) {
            return false;
        }
    }
}
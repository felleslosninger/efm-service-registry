package no.difi.meldingsutveksling.serviceregistry.service.elma;

import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryException;
import no.difi.meldingsutveksling.serviceregistry.config.ServiceregistryProperties;
import no.difi.meldingsutveksling.serviceregistry.exceptions.EndpointUrlNotFound;
import no.difi.vefa.peppol.common.lang.EndpointNotFoundException;
import no.difi.vefa.peppol.common.model.*;
import no.difi.vefa.peppol.lookup.LookupClient;
import no.difi.vefa.peppol.lookup.api.LookupException;

import no.difi.vefa.peppol.security.api.PeppolSecurityException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * See configuraion beans for beans that might be injected as LookupClient and TrasnportProfile
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

    public Endpoint lookup(String organisationNumber) {
        try {
            return lookupClient.getEndpoint(new ParticipantIdentifier(organisationNumber),
                    new DocumentTypeIdentifier(props.getElma().getDocumentTypeIdentifier()),
                    new ProcessIdentifier(props.getElma().getProcessIdentifier()),
                    transportProfile);
        } catch (PeppolSecurityException e) {
            throw new ServiceRegistryException(e);
        } catch (LookupException | EndpointNotFoundException e) {
            throw new EndpointUrlNotFound(String.format("Failed lookup through ELMA of %s", organisationNumber), e);
        }
    }
}
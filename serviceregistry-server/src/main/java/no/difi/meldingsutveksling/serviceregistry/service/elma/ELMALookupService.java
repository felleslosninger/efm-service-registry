package no.difi.meldingsutveksling.serviceregistry.service.elma;

import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryException;
import no.difi.meldingsutveksling.serviceregistry.exceptions.EndpointUrlNotFound;
import no.difi.vefa.peppol.common.lang.EndpointNotFoundException;
import no.difi.vefa.peppol.common.model.*;
import no.difi.vefa.peppol.lookup.LookupClient;
import no.difi.vefa.peppol.lookup.api.LookupException;
import no.difi.vefa.peppol.security.lang.PeppolSecurityException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * See configuraion beans for beans that might be injected as LookupClient and TransportProfile
 */
@Component
public class ELMALookupService {

    private static final ProcessIdentifier PROCESS_IDENTIFIER = ProcessIdentifier.of("urn:www.difi.no:profile:staging-meldingsutveksling:ver1.0");
    private static final DocumentTypeIdentifier DOCUMENT_IDENTIFIER = DocumentTypeIdentifier.of("urn:no:difi:staging-meldingsuveksling:xsd::Melding##urn:www.difi.no:staging-meldingsutveksling:melding:1.0:extended:urn:www.difi.no:encoded:aes-zip:1.0::1.0");

    private LookupClient lookupClient;
    private TransportProfile transportProfile;

    @Autowired
    public ELMALookupService(LookupClient lookupClient, TransportProfile transportProfile) {
        this.lookupClient = lookupClient;
        this.transportProfile = transportProfile;
    }

    public Endpoint lookup(String organisationNumber) {
        try {
            return lookupClient.getEndpoint(ParticipantIdentifier.of(organisationNumber),
                    DOCUMENT_IDENTIFIER,
                    PROCESS_IDENTIFIER,
                    transportProfile);
        } catch (PeppolSecurityException e) {
            throw new ServiceRegistryException(e);
        } catch (LookupException | EndpointNotFoundException e) {
            throw new EndpointUrlNotFound(String.format("Failed lookup through ELMA of %s", organisationNumber), e);
        }
    }
}
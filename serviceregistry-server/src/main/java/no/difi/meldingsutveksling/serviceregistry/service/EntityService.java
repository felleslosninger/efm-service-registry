package no.difi.meldingsutveksling.serviceregistry.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.domain.FiksIoIdentifier;
import no.difi.meldingsutveksling.domain.Iso6523;
import no.difi.meldingsutveksling.domain.NhnIdentifier;
import no.difi.meldingsutveksling.domain.PartnerIdentifier;
import no.difi.meldingsutveksling.domain.PersonIdentifier;
import no.difi.meldingsutveksling.serviceregistry.CacheConfig;
import no.difi.meldingsutveksling.serviceregistry.SRRequestScope;
import no.difi.meldingsutveksling.serviceregistry.domain.CitizenInfo;
import no.difi.meldingsutveksling.serviceregistry.domain.EntityInfo;
import no.difi.meldingsutveksling.serviceregistry.domain.FiksIoInfo;
import no.difi.meldingsutveksling.serviceregistry.domain.HelseEnhetInfo;
import no.difi.meldingsutveksling.serviceregistry.exceptions.EntityNotFoundException;
import no.difi.meldingsutveksling.serviceregistry.fiks.io.FiksIoService;
import no.difi.meldingsutveksling.serviceregistry.record.LookupParameters;
import no.difi.meldingsutveksling.serviceregistry.service.brreg.BrregNotFoundException;
import no.difi.meldingsutveksling.serviceregistry.service.brreg.BrregService;
import no.difi.meldingsutveksling.serviceregistry.service.healthcare.HealthAddressRegistryDetails;
import no.difi.meldingsutveksling.serviceregistry.service.healthcare.NhnService;
import no.ks.fiks.io.client.model.Konto;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;

import java.util.Optional;

import static no.difi.meldingsutveksling.serviceregistry.logging.SRMarkerFactory.markerFrom;

/**
 * Service is used to lookup information needed to send messages to an entity.
 * It can be a citizen or an organization
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EntityService {

    private final BrregService brregService;
    private final ObjectProvider<FiksIoService> fiksIoService;
    private final SRRequestScope requestScope;
    private final NhnService nhnService;

    public Optional<EntityInfo> getEntityInfo(PartnerIdentifier partnerIdentifier) {
        String identifier = partnerIdentifier.getIdentifier();

        switch (partnerIdentifier) {
            case Iso6523 iso6523 -> {
                try {
                    return brregService.getOrganizationInfo(iso6523);
                } catch (BrregNotFoundException e) {
                    log.error(markerFrom(requestScope), "Could not find entity for the requested identifier={}: {}", identifier, e.getMessage());
                    return Optional.empty();
                }
            }
            case PersonIdentifier _ -> {
                return Optional.of(new CitizenInfo(identifier));
            }
            case FiksIoIdentifier _ -> {
                if (fiksIoService.getIfAvailable() != null) {
                    return fiksIoService.getIfAvailable().lookup(identifier)
                            .filter(Konto::isGyldigMottaker)
                            .map(k -> new FiksIoInfo(identifier));
                }
                return Optional.empty();
            }
            case NhnIdentifier nhnIdentifier -> {
                return getAddressRegisterDetails(nhnIdentifier).map(p -> toHelseEnhetInfo(nhnIdentifier, p));
            }
        }
    }

    private HelseEnhetInfo toHelseEnhetInfo(NhnIdentifier nhnIdentifier, HealthAddressRegistryDetails details) {
        String patient = nhnIdentifier.getType() == NhnIdentifier.Type.FASTLEGE_FOR ? nhnIdentifier.getId() : null;
        return new HelseEnhetInfo(nhnIdentifier.getIdentifier(), details.getParentHerId(), details.getParentName(), details.getHerId(), details.getName(), details.getOrgNumber(), patient);
    }

    private Optional<HealthAddressRegistryDetails> getAddressRegisterDetails(NhnIdentifier nhnIdentifier) {
        String identifier = nhnIdentifier.getIdentifier();
        try {
            return Optional.of(nhnService.getARDetails(LookupParameters.lookup(identifier).setToken(requestScope.getToken())));
        } catch (EntityNotFoundException e) {
            log.info("The identifier is not found in address register {}", identifier);
            return Optional.empty();
        } catch (ResourceAccessException e) {
            log.warn("Healthcare service is down, isNhnRegistered check returning false", e);
            return Optional.empty();
        }
    }
}

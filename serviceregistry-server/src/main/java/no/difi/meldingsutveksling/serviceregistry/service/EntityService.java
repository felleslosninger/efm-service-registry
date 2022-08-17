package no.difi.meldingsutveksling.serviceregistry.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.domain.FiksIoIdentifier;
import no.difi.meldingsutveksling.domain.Iso6523;
import no.difi.meldingsutveksling.domain.PartnerIdentifier;
import no.difi.meldingsutveksling.domain.PersonIdentifier;
import no.difi.meldingsutveksling.serviceregistry.CacheConfig;
import no.difi.meldingsutveksling.serviceregistry.SRRequestScope;
import no.difi.meldingsutveksling.serviceregistry.domain.*;
import no.difi.meldingsutveksling.serviceregistry.fiks.io.FiksIoService;
import no.difi.meldingsutveksling.serviceregistry.service.brreg.BrregNotFoundException;
import no.difi.meldingsutveksling.serviceregistry.service.brreg.BrregService;
import no.ks.fiks.io.client.model.Konto;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Optional;

import static no.difi.meldingsutveksling.serviceregistry.logging.SRMarkerFactory.markerFrom;

/**
 * Service is used to lookup information needed to send messages to an entity
 * An entity can be a citizen or an organization
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EntityService {

    private final BrregService brregService;
    private final ObjectProvider<FiksIoService> fiksIoService;
    private final SRRequestScope requestScope;

    /**
     * @param identifier for an entity either an organization number or a personal identification number
     * @return info needed to send messages to the entity
     */
    @Cacheable(CacheConfig.BRREG_CACHE)
    public Optional<EntityInfo> getEntityInfo(PartnerIdentifier identifier) {
        if (identifier instanceof PersonIdentifier) {
            return Optional.of(new CitizenInfo(identifier.getIdentifier()));
        } else if (identifier instanceof FiksIoIdentifier) {
            if (fiksIoService.getIfAvailable() != null) {
                return fiksIoService.getIfAvailable().lookup(identifier.cast(FiksIoIdentifier.class))
                        .filter(Konto::isGyldigMottaker)
                        .map(k -> new FiksIoInfo(identifier.getIdentifier()));
            }
            return Optional.empty();
        } else if (identifier instanceof Iso6523) {
            try {
                if (identifier.hasOrganizationPartIdentifier()) {
//                    TODO: Valider utvidet adresse mot maskinporten når API for det er tilgjengelig
                    if(brregService.getOrganizationInfo(identifier.cast(Iso6523.class)).isPresent()) {
                        return Optional.of(new OrganizationInfo(identifier.getIdentifier(), new OrganizationType("")));
                    } else{
                        return Optional.empty();
                    }
                } else {
                    return brregService.getOrganizationInfo(identifier.cast(Iso6523.class));
                }
            } catch (BrregNotFoundException e) {
                log.error(markerFrom(requestScope), "Could not find entity for the requested identifier={}: {}", identifier, e.getMessage());
                return Optional.empty();
            }
        } else {
            return Optional.empty();
        }

    }

}

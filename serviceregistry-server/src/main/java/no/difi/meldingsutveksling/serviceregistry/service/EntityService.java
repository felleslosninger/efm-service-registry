package no.difi.meldingsutveksling.serviceregistry.service;

import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.serviceregistry.CacheConfig;
import no.difi.meldingsutveksling.serviceregistry.model.CitizenInfo;
import no.difi.meldingsutveksling.serviceregistry.model.EntityInfo;
import no.difi.meldingsutveksling.serviceregistry.service.brreg.BrregNotFoundException;
import no.difi.meldingsutveksling.serviceregistry.service.brreg.BrregService;
import no.difi.meldingsutveksling.serviceregistry.util.SRRequestScope;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Optional;

import static no.difi.meldingsutveksling.serviceregistry.businesslogic.ServiceRecordPredicates.isCitizen;
import static no.difi.meldingsutveksling.serviceregistry.logging.SRMarkerFactory.markerFrom;

/**
 * Service is used to lookup information needed to send messages to an entity
 * An entity can be a citizen or an organization
 */
@Service
@Slf4j
public class EntityService {

    private final BrregService brregService;
    private final SRRequestScope requestScope;

    public EntityService(BrregService brregService,
                         SRRequestScope requestScope) {
        this.brregService = brregService;
        this.requestScope = requestScope;
    }

    /**
     * @param identifier for an entity either an organization number or a personal identification number
     * @return info needed to send messages to the entity
     */
    @Cacheable(CacheConfig.BRREG_CACHE)
    public Optional<EntityInfo> getEntityInfo(String identifier) {
        if (isCitizen().test(identifier)) {
            return Optional.of(new CitizenInfo(identifier));
        } else {
            try {
                return brregService.getOrganizationInfo(identifier);
            } catch (BrregNotFoundException e) {
                log.error(markerFrom(requestScope), "Could not find entity for the requested identifier={}", identifier, e);
                return Optional.empty();
            }
        }
    }

}

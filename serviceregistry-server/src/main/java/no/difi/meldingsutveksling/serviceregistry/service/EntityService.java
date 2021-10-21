package no.difi.meldingsutveksling.serviceregistry.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.serviceregistry.CacheConfig;
import no.difi.meldingsutveksling.serviceregistry.domain.CitizenInfo;
import no.difi.meldingsutveksling.serviceregistry.domain.EntityInfo;
import no.difi.meldingsutveksling.serviceregistry.domain.FiksIoInfo;
import no.difi.meldingsutveksling.serviceregistry.fiks.io.FiksIoService;
import no.difi.meldingsutveksling.serviceregistry.service.brreg.BrregNotFoundException;
import no.difi.meldingsutveksling.serviceregistry.service.brreg.BrregService;
import no.difi.meldingsutveksling.serviceregistry.SRRequestScope;
import no.ks.fiks.io.client.model.Konto;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Optional;

import static no.difi.meldingsutveksling.serviceregistry.businesslogic.ServiceRecordPredicates.isCitizen;
import static no.difi.meldingsutveksling.serviceregistry.businesslogic.ServiceRecordPredicates.isUuid;
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
    public Optional<EntityInfo> getEntityInfo(String identifier) {
        if (isCitizen().test(identifier)) {
            return Optional.of(new CitizenInfo(identifier));
        } else if (isUuid().test(identifier)) {
            if (fiksIoService.getIfAvailable() != null) {
                return fiksIoService.getIfAvailable().lookup(identifier)
                    .filter(Konto::isGyldigMottaker)
                    .map(k -> new FiksIoInfo(identifier));
            }
            return Optional.empty();
        } else {
            try {
                return brregService.getOrganizationInfo(identifier);
            } catch (BrregNotFoundException e) {
                log.error(markerFrom(requestScope), "Could not find entity for the requested identifier={}: {}", identifier, e.getMessage());
                return Optional.empty();
            }
        }
    }

}

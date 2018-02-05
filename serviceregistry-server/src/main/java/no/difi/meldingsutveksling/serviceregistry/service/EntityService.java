package no.difi.meldingsutveksling.serviceregistry.service;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import no.difi.meldingsutveksling.serviceregistry.model.CitizenInfo;
import no.difi.meldingsutveksling.serviceregistry.model.EntityInfo;
import no.difi.meldingsutveksling.serviceregistry.service.brreg.BrregService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static no.difi.meldingsutveksling.serviceregistry.businesslogic.ServiceRecordPredicates.isCitizen;

/**
 * Service is used to lookup information needed to send messages to an entity
 * An entity can be a citizen or an organization
 */
@Service
public class EntityService {

    LoadingCache<String, Optional<EntityInfo>> entityCache;

    private final BrregService brregService;

    @Autowired
    public EntityService(BrregService brregService) {
        this.brregService = brregService;

        this.entityCache = CacheBuilder.newBuilder()
                .expireAfterWrite(1, TimeUnit.DAYS)
                .build(new CacheLoader<String, Optional<EntityInfo>>() {
                    @Override
                    public Optional<EntityInfo> load(String key) throws Exception {
                        return loadEntityInfo(key);
                    }
                });
    }

    /**
     *
     * @param identifier for an entity either an organization number or a fodselsnummer
     * @return info needed to send messages to the entity
     */
    private Optional<EntityInfo> loadEntityInfo(String identifier) {
        if (isCitizen().test(identifier)) {
            return Optional.of(new CitizenInfo(identifier));
        } else {
            return brregService.getOrganizationInfo(identifier);
        }
    }

    /**
     *
     * @param identifier for an entity either an organization number or a fodselsnummer
     * @return info needed to send messages to the entity
     */
    public Optional<EntityInfo> getEntityInfo(String identifier) {
        return this.entityCache.getUnchecked(identifier);
    }

}

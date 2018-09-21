package no.difi.meldingsutveksling.serviceregistry.model;


import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.Data;
import no.difi.meldingsutveksling.serviceregistry.servicerecord.ServiceRecord;

import java.util.List;
import java.util.Map;

/**
 * Represents an organization or a citizen
 */
@Data
public class Entity {

    private EntityInfo infoRecord;
    private ServiceRecord serviceRecord;
    private List<ServiceRecord> serviceRecords;
    private List<ServiceIdentifier> failedServiceIdentifiers = Lists.newArrayList();
    private Map<ServiceIdentifier, Integer> securitylevels = Maps.newEnumMap(ServiceIdentifier.class);

    /**
     * Creates an empty Entity
     */
    public Entity() {
        serviceRecords = Lists.newArrayList();
    }
}

package no.difi.meldingsutveksling.serviceregistry.domain;


import com.google.common.collect.Lists;
import lombok.Data;
import no.difi.meldingsutveksling.serviceregistry.record.ServiceRecord;

import java.util.List;

/**
 * Represents an organization or a citizen
 */
@Data
public class Entity {

    private EntityInfo infoRecord;
    private List<ServiceRecord> serviceRecords;

    /**
     * Creates an empty Entity
     */
    public Entity() {
        serviceRecords = Lists.newArrayList();
    }
}

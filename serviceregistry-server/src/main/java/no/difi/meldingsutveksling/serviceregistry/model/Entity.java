package no.difi.meldingsutveksling.serviceregistry.model;


import no.difi.meldingsutveksling.serviceregistry.servicerecord.ServiceRecord;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an organization or a citizen
 */
public class Entity {

    private EntityInfo info;
    private List<ServiceRecord> serviceRecords;

    /**
     * Creates an empty Entity
     */
    public Entity() {
        serviceRecords = new ArrayList<>();
    }

    public void addServiceRecord(ServiceRecord s) {
        serviceRecords.add(s);
    }

    public EntityInfo getInfo() {
        return info;
    }

    public void setInfo(EntityInfo info) {
        this.info = info;
    }

    public List<ServiceRecord> getServiceRecords() {
        return serviceRecords;
    }

    public void setServiceRecords(List<ServiceRecord> serviceRecords) {
        this.serviceRecords = serviceRecords;
    }

    @Override
    public String toString() {
        return "Organisation{" +
                "info=" + info +
                ", serviceRecords=" + serviceRecords +
                '}';
    }
}

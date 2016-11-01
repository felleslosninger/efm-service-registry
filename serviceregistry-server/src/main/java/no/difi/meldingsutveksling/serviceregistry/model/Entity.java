package no.difi.meldingsutveksling.serviceregistry.model;


import com.google.common.base.MoreObjects;
import no.difi.meldingsutveksling.serviceregistry.servicerecord.ServiceRecord;

/**
 * Represents an organization or a citizen
 */
public class Entity {

    private EntityInfo info;
    private ServiceRecord serviceRecord;

    /**
     * Creates an empty Entity
     */
    public Entity() {
    }

    public void setServiceRecord(ServiceRecord s) {
        this.serviceRecord = s;
    }

    public ServiceRecord getServiceRecord() {
        return serviceRecord;
    }

    public EntityInfo getInfo() {
        return info;
    }

    public void setInfo(EntityInfo info) {
        this.info = info;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("info", info)
                .add("serviceRecord", serviceRecord)
                .toString();
    }
}

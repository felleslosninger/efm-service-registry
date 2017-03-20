package no.difi.meldingsutveksling.serviceregistry.model;


import com.google.common.base.MoreObjects;
import no.difi.meldingsutveksling.serviceregistry.servicerecord.ServiceRecord;

/**
 * Represents an organization or a citizen
 */
public class Entity {

    private EntityInfo infoRecord;
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

    public EntityInfo getInfoRecord() {
        return infoRecord;
    }

    public void setInfoRecord(EntityInfo info) {
        this.infoRecord = info;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("info", infoRecord)
                .add("serviceRecord", serviceRecord)
                .toString();
    }
}

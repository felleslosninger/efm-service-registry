package no.difi.meldingsutveksling.serviceregistry.controller;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import no.difi.meldingsutveksling.serviceregistry.model.Entity;
import no.difi.meldingsutveksling.serviceregistry.model.EntityInfo;
import no.difi.meldingsutveksling.serviceregistry.servicerecord.ServiceRecord;
import org.springframework.hateoas.ResourceSupport;

public class EntityResource extends ResourceSupport {

    private ServiceRecord serviceRecord;
    private Entity entity;

    /**
     * Creates a resource object that will be returned to the REST client, based the the domain object
     *
     * @param entity the domain object to be used to creat this REST resource
     */
    @JsonCreator
    public EntityResource(@JsonProperty("entity") Entity entity) {
        this.entity = entity;
        this.serviceRecord = entity.getServiceRecord();
    }

    public ServiceRecord getServiceRecord() {
        return serviceRecord;
    }

    public void setServiceRecord(ServiceRecord serviceRecord) {
        this.serviceRecord = serviceRecord;
    }

    public EntityInfo getInfoRecord() {
        return entity.getInfo();
    }

}


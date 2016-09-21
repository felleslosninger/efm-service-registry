package no.difi.meldingsutveksling.serviceregistry.controller;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import no.difi.meldingsutveksling.serviceregistry.model.Entity;
import no.difi.meldingsutveksling.serviceregistry.model.EntityInfo;
import no.difi.meldingsutveksling.serviceregistry.servicerecord.ServiceRecord;
import org.springframework.hateoas.ResourceSupport;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

public class EntityResource extends ResourceSupport {

    private List<ServiceRecordResource> serviceRecords;
    private Entity entity;

    /**
     * Creates a resource object that will be returned to the REST client, based the the domain object
     *
     * @param entity the domain object to be used to creat this REST resource
     */
    @JsonCreator
    public EntityResource(@JsonProperty("entity") Entity entity) {
        this.entity = entity;
        serviceRecords = new ArrayList<>();
        for (ServiceRecord r : entity.getServiceRecords()) {
            final ServiceRecordResource e = new ServiceRecordResource(r);
            final Object invocationValue = methodOn(ServiceRecordController.class).setPrimary(entity.getInfo().getIdentifier(),
                    r.getServiceIdentifier().getName());
            e.add(linkTo(invocationValue).withRel("setprimary"));
            serviceRecords.add(e);
        }
    }

    public List<ServiceRecordResource> getServiceRecords() {
        return serviceRecords;
    }

    public void setServiceRecords(List<ServiceRecordResource> serviceRecords) {
        this.serviceRecords = serviceRecords;
    }

    public EntityInfo getInfoRecord() {
        return entity.getInfo();
    }

}


package no.difi.meldingsutveksling.serviceregistry.controller;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import no.difi.meldingsutveksling.serviceregistry.model.OrganizationInfo;
import no.difi.meldingsutveksling.serviceregistry.model.Organization;
import no.difi.meldingsutveksling.serviceregistry.servicerecord.ServiceRecord;
import org.springframework.hateoas.ResourceSupport;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

public class OrganizationResource extends ResourceSupport {

    private List<ServiceRecordResource> serviceRecords;
    private Organization organization;

    /**
     * Creates a resource object that will be returned to the REST client, based the the domain object
     *
     * @param organization the domain object to be used to creat this REST resource
     */
    @JsonCreator
    public OrganizationResource(@JsonProperty("organization") Organization organization) {
        this.organization = organization;
        serviceRecords = new ArrayList<>();
        for (ServiceRecord r : organization.getServiceRecords()) {
            final ServiceRecordResource e = new ServiceRecordResource(r);
            final Object invocationValue = methodOn(ServiceRecordController.class).setPrimary(organization.getInfo().getOrganisationNumber(),
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

    public OrganizationInfo getInfoRecord() {
        return organization.getInfo();
    }

}


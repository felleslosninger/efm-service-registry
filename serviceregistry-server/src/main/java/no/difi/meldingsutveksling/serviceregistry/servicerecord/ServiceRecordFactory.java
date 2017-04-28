package no.difi.meldingsutveksling.serviceregistry.servicerecord;

import no.difi.meldingsutveksling.Notification;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.meldingsutveksling.logging.MarkerFactory;
import no.difi.meldingsutveksling.ptp.PostAddress;
import no.difi.meldingsutveksling.serviceregistry.CertificateNotFoundException;
import no.difi.meldingsutveksling.serviceregistry.config.ServiceregistryProperties;
import no.difi.meldingsutveksling.serviceregistry.exceptions.EndpointUrlNotFound;
import no.difi.meldingsutveksling.serviceregistry.krr.DSFResource;
import no.difi.meldingsutveksling.serviceregistry.krr.KRRClientException;
import no.difi.meldingsutveksling.serviceregistry.krr.PersonResource;
import no.difi.meldingsutveksling.serviceregistry.model.ServiceIdentifier;
import no.difi.meldingsutveksling.serviceregistry.service.elma.ELMALookupService;
import no.difi.meldingsutveksling.serviceregistry.service.krr.KrrService;
import no.difi.meldingsutveksling.serviceregistry.service.ks.FiksAdressing;
import no.difi.meldingsutveksling.serviceregistry.service.virksert.VirkSertService;
import no.difi.vefa.peppol.common.model.Endpoint;
import no.difi.virksert.client.lang.VirksertClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

import java.lang.invoke.MethodHandles;

import static no.difi.meldingsutveksling.serviceregistry.krr.LookupParameters.lookup;
import static no.difi.meldingsutveksling.serviceregistry.krr.PersonResource.Reservasjon.NEI;
import static no.difi.meldingsutveksling.serviceregistry.model.ServiceIdentifier.DPE_data;
import static no.difi.meldingsutveksling.serviceregistry.model.ServiceIdentifier.DPE_innsyn;

/**
 * Factory method class to create Service Records based on lookup endpoint urls and certificates corresponding to those services
 */
@Component
public class ServiceRecordFactory {

    private final KrrService krrService;
    private ServiceregistryProperties properties;
    private VirkSertService virksertService;
    private ELMALookupService elmaLookupService;
    private static final String NORWAY_PREFIX = "9908:";
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass().getName());

    /**
     * Creates factory to create ServiceRecord using provided environment and services
     *
     * @param properties - parameters needed to contact the provided services
     * @param virksertService - used to lookup virksomhetssertifikat (certificate)
     * @param elmaLookupService - used to lookup hostname of Altinn formidlingstjeneste
     * @param krrService - used to lookup parameters needed to use DPI transportation
     */
    @Autowired
    public ServiceRecordFactory(ServiceregistryProperties properties, VirkSertService virksertService, ELMALookupService elmaLookupService, KrrService krrService) {
        this.properties = properties;
        this.virksertService = virksertService;
        this.elmaLookupService = elmaLookupService;
        this.krrService = krrService;
    }

    public ServiceRecord createFiksServiceRecord(FiksAdressing fiksAdressing) {
        return new FiksServiceRecord(fiksAdressing);
    }

    @PreAuthorize("#oauth2.hasScope('move/dpo.read')")
    public ServiceRecord createEduServiceRecord(String orgnr) {
        Endpoint ep;
        try {
            ep = elmaLookupService.lookup(NORWAY_PREFIX + orgnr);
        } catch (EndpointUrlNotFound endpointUrlNotFound) {
            logger.info(MarkerFactory.receiverMarker(orgnr),
                    String.format("Attempted to lookup receiver in ELMA: %s", endpointUrlNotFound.getMessage()));
            Audit.info("Does not exist in ELMA (no IP?) -> DPV will be used", MarkerFactory.receiverMarker(orgnr));
            return createPostVirksomhetServiceRecord(orgnr);
        }
        String pemCertificate = lookupPemCertificate(orgnr);

        EDUServiceRecord serviceRecord = new EDUServiceRecord(pemCertificate, ep.getAddress().toString(), orgnr);

        String adr = ep.getAddress().toString();
        if (adr.contains("#")) {
            String uri = adr.substring(0, adr.indexOf('#'));
            String[] codes = adr.substring(adr.indexOf('#') + 1).split("-");
            String serviceCode = codes[0];
            String serviceEditionCode = codes[1];

            serviceRecord.setEndpointUrl(uri);
            serviceRecord.setServiceCode(serviceCode);
            serviceRecord.setServiceEditionCode(serviceEditionCode);
        } else {
            serviceRecord.setEndpointUrl(adr);
        }

        if (elmaLookupService.identifierHasInnsynskravCapability(NORWAY_PREFIX + orgnr)) {
            serviceRecord.addDpeCapability(DPE_innsyn.toString());
        }
        if (elmaLookupService.identifierHasInnsynDataCapability(NORWAY_PREFIX + orgnr)) {
            serviceRecord.addDpeCapability(DPE_data.toString());
        }

        return serviceRecord;
    }

    @PreAuthorize("#oauth2.hasScope('move/dpv.read')")
    public ServiceRecord createPostVirksomhetServiceRecord(String orgnr) {
        return new PostVirksomhetServiceRecord(properties, orgnr);
    }

    private String lookupPemCertificate(String orgnumber) {
        try {
            return virksertService.getCertificate(orgnumber);
        } catch (VirksertClientException e) {
            throw new CertificateNotFoundException(String.format("Unable to find certificate for: %s", orgnumber), e);
        }
    }

    @PreAuthorize("#oauth2.hasScope('move/dpi.read')")
    public ServiceRecord createServiceRecordForCititzen(String identifier,
                                                        String token,
                                                        String onBehalfOrgnr,
                                                        Notification notification) throws KRRClientException {

        PersonResource personResource = krrService.getCizitenInfo(lookup(identifier)
                .onBehalfOf(onBehalfOrgnr)
                .require(notification)
                .token(token));

        if (!personResource.hasMailbox() && NEI.name().equals(personResource.getReserved())) {
            return createPostVirksomhetServiceRecord(identifier);
        }

        DSFResource dsfResource = krrService.getDSFInfo(identifier, token);
        String[] codeArea = dsfResource.getPostAddress().split(" ");
        PostAddress postAddress = new PostAddress(dsfResource.getName(),
                dsfResource.getStreet(),
                codeArea[0],
                codeArea.length > 1 ? codeArea[1] : codeArea[0],
                dsfResource.getCountry());

        return new SikkerDigitalPostServiceRecord(properties, personResource, ServiceIdentifier.DPI, identifier,
                postAddress, postAddress);
    }

}

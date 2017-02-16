package no.difi.meldingsutveksling.serviceregistry.servicerecord;

import no.difi.meldingsutveksling.Notification;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.meldingsutveksling.logging.MarkerFactory;
import no.difi.meldingsutveksling.ptp.KontaktInfo;
import no.difi.meldingsutveksling.ptp.PostAddress;
import no.difi.meldingsutveksling.ptp.Street;
import no.difi.meldingsutveksling.serviceregistry.CertificateNotFoundException;
import no.difi.meldingsutveksling.serviceregistry.config.ServiceregistryProperties;
import no.difi.meldingsutveksling.serviceregistry.exceptions.EndpointUrlNotFound;
import no.difi.meldingsutveksling.serviceregistry.model.ServiceIdentifier;
import no.difi.meldingsutveksling.serviceregistry.service.elma.ELMALookupService;
import no.difi.meldingsutveksling.serviceregistry.service.krr.KrrService;
import no.difi.meldingsutveksling.serviceregistry.service.ks.KSLookup;
import no.difi.meldingsutveksling.serviceregistry.service.virksert.VirkSertService;
import no.difi.vefa.peppol.common.model.Endpoint;
import no.difi.virksert.client.VirksertClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

import java.lang.invoke.MethodHandles;

import static no.difi.meldingsutveksling.serviceregistry.krr.LookupParameters.lookup;

/**
 * Factory method class to create Service Records based on lookup endpoint urls and certificates corresponding to those services
 */
@Component
public class ServiceRecordFactory {

    private final KrrService krrService;
    private ServiceregistryProperties properties;
    private VirkSertService virksertService;
    private ELMALookupService elmaLookupService;
    private KSLookup ksLookup;
    private static final String NORWAY_PREFIX = "9908:";
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass().getName());

    /**
     * Creates factory to create ServiceRecord using provided environment and services
     *
     * @param properties - parameters needed to contact the provided services
     * @param virksertService - used to lookup virksomhetssertifikat (certificate)
     * @param elmaLookupService - used to lookup hostname of Altinn formidlingstjeneste
     * @param ksLookup - used to lookup if ks should be used for transportation
     * @param krrService - used to lookup parameters needed to use DPI transportation
     */
    @Autowired
    public ServiceRecordFactory(ServiceregistryProperties properties, VirkSertService virksertService, ELMALookupService elmaLookupService, KSLookup ksLookup, KrrService krrService) {
        this.properties = properties;
        this.virksertService = virksertService;
        this.elmaLookupService = elmaLookupService;
        this.ksLookup = ksLookup;
        this.krrService = krrService;
    }

    @PreAuthorize("#oauth2.hasScope('move/dpo.read')")
    public ServiceRecord createEduServiceRecord(String orgnr) {
        String finalOrgNumber = ksLookup.mapOrganizationNumber(orgnr);
        Endpoint ep;
        try {
            ep = elmaLookupService.lookup(NORWAY_PREFIX + finalOrgNumber);
        } catch (EndpointUrlNotFound endpointUrlNotFound) {
            Audit.info("Does not exist in ELMA (no IP?) -> DPV will be used", MarkerFactory.receiverMarker(orgnr));
            logger.warn(MarkerFactory.receiverMarker(orgnr), "Attempted to lookup receiver in ELMA", endpointUrlNotFound);

            return createPostVirksomhetServiceRecord(orgnr);
        }
        String pemCertificate = lookupPemCertificate(finalOrgNumber);

        String adr = ep.getAddress().toString();
        if (adr.contains("#")) {
            String uri = adr.substring(0, adr.indexOf('#'));
            String[] codes = adr.substring(adr.indexOf('#')+1).split("-");
            String serviceCode = codes[0];
            String serviceEditionCode = codes[1];
            return new EDUServiceRecord(properties, pemCertificate, uri, serviceCode, serviceEditionCode, orgnr);
        }

        return new EDUServiceRecord(properties, pemCertificate, ep.getAddress().toString(), orgnr);
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
    public ServiceRecord createServiceRecordForCititzen(String identifier, String clientOrgnr, Notification obligation) {

        final KontaktInfo kontaktInfo = krrService.getCitizenInfo(
                lookup(identifier)
                        .onBehalfOf(clientOrgnr)
                        .require(obligation));
        PostAddress postAddress = new PostAddress("DIFI", new Street("Grev Wedels plass 9", "", "", ""), "0151", "Oslo", "Norway");
        if (!kontaktInfo.hasMailbox() && !kontaktInfo.isReservert()) {
            return createPostVirksomhetServiceRecord(identifier);
        }
        return new SikkerDigitalPostServiceRecord(properties, kontaktInfo, ServiceIdentifier.DPI, identifier, postAddress, postAddress);
    }
}

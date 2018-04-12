package no.difi.meldingsutveksling.serviceregistry.servicerecord;

import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.Notification;
import no.difi.meldingsutveksling.logging.MarkerFactory;
import no.difi.meldingsutveksling.serviceregistry.CertificateNotFoundException;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryException;
import no.difi.meldingsutveksling.serviceregistry.config.ServiceregistryProperties;
import no.difi.meldingsutveksling.serviceregistry.exceptions.EndpointUrlNotFound;
import no.difi.meldingsutveksling.serviceregistry.krr.DSFResource;
import no.difi.meldingsutveksling.serviceregistry.krr.KRRClientException;
import no.difi.meldingsutveksling.serviceregistry.krr.PersonResource;
import no.difi.meldingsutveksling.serviceregistry.krr.PostAddress;
import no.difi.meldingsutveksling.serviceregistry.model.EntityInfo;
import no.difi.meldingsutveksling.serviceregistry.model.OrganizationInfo;
import no.difi.meldingsutveksling.serviceregistry.model.ServiceIdentifier;
import no.difi.meldingsutveksling.serviceregistry.service.EntityService;
import no.difi.meldingsutveksling.serviceregistry.service.elma.ELMALookupService;
import no.difi.meldingsutveksling.serviceregistry.service.krr.KrrService;
import no.difi.meldingsutveksling.serviceregistry.service.virksert.VirkSertService;
import no.difi.meldingsutveksling.serviceregistry.svarut.SvarUtService;
import no.difi.vefa.peppol.common.model.Endpoint;
import no.difi.virksert.client.lang.VirksertClientException;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetails;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static no.difi.meldingsutveksling.serviceregistry.krr.LookupParameters.lookup;
import static no.difi.meldingsutveksling.serviceregistry.krr.PersonResource.Reservasjon.NEI;
import static no.difi.meldingsutveksling.serviceregistry.model.ServiceIdentifier.*;

/**
 * Factory method class to create Service Records based on lookup endpoint urls and certificates corresponding to those services
 */
@Component
@Slf4j
public class ServiceRecordFactory {

    private final KrrService krrService;
    private ServiceregistryProperties properties;
    private VirkSertService virksertService;
    private ELMALookupService elmaLookupService;
    private EntityService entityService;
    private SvarUtService svarUtService;
    private static final String NORWAY_PREFIX = "9908:";

    /**
     * Creates factory to create ServiceRecord using provided environment and services
     *
     * @param properties - parameters needed to contact the provided services
     * @param virksertService - used to lookup virksomhetssertifikat (certificate)
     * @param elmaLookupService - used to lookup hostname of Altinn formidlingstjeneste
     * @param krrService - used to lookup parameters needed to use DPI transportation
     */
    @Autowired
    public ServiceRecordFactory(ServiceregistryProperties properties,
                                VirkSertService virksertService,
                                ELMALookupService elmaLookupService,
                                KrrService krrService,
                                EntityService entityService,
                                SvarUtService svarUtService) {
        this.properties = properties;
        this.virksertService = virksertService;
        this.elmaLookupService = elmaLookupService;
        this.krrService = krrService;
        this.entityService = entityService;
        this.svarUtService = svarUtService;
    }

    public Optional<ServiceRecord> createFiksServiceRecord(String orgnr) {
        if (svarUtService.getSvarUtAdressering(orgnr)) {
            String pem;
            try {
                pem = IOUtils.toString(properties.getSvarut().getCertificate().getInputStream(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                log.error("Could not read certificate from {}", properties.getSvarut().getCertificate().toString());
                throw new ServiceRegistryException(e);
            }
            return Optional.of(new FiksServiceRecord(orgnr, pem, properties.getSvarut().getForsendelsesserviceUrl().toString()));
        }
        return Optional.empty();
    }

    @SuppressWarnings("squid:S1166") // Suppress Sonar due to no rethrow/log from certificate ex.
    public Optional<ServiceRecord> createEduServiceRecord(String orgnr) {
        Endpoint ep;
        try {
            ep = elmaLookupService.lookup(NORWAY_PREFIX + orgnr);
        } catch (EndpointUrlNotFound endpointUrlNotFound) {
            log.info(MarkerFactory.receiverMarker(orgnr),
                    String.format("Attempted to lookup receiver in ELMA: %s", endpointUrlNotFound.getMessage()));
            return Optional.empty();
        }

        String pemCertificate;
        try {
            pemCertificate = lookupPemCertificate(orgnr);
        } catch (CertificateNotFoundException e) {
            log.info(MarkerFactory.receiverMarker(orgnr), String.format("Identifier %s found in ELMA with " +
                    "DPO profile, but certificate not found in Virksert.", orgnr));
            return Optional.empty();
        }

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
            serviceRecord.addDpeCapability(DPE_INNSYN.toString());
        }
        if (elmaLookupService.identifierHasInnsynDataCapability(NORWAY_PREFIX + orgnr)) {
            serviceRecord.addDpeCapability(DPE_DATA.toString());
        }

        return Optional.of(serviceRecord);
    }

    @SuppressWarnings("squid:S1166") // Suppress Sonar due to no rethrow/log from certificate ex.
    public Optional<ServiceRecord> createDpeInnsynServiceRecord(String orgnr) {
        if (elmaLookupService.identifierHasInnsynskravCapability(NORWAY_PREFIX + orgnr)) {
            String pemCertificate;
            try {
                pemCertificate = lookupPemCertificate(orgnr);
            } catch (CertificateNotFoundException e) {
                log.info(MarkerFactory.receiverMarker(orgnr), String.format("Identifier %s found in ELMA with " +
                        "DPE Innsyn profile, but certificate not found in Virksert.", orgnr));
                return Optional.empty();
            }
            DpeServiceRecord sr = DpeServiceRecord.of(pemCertificate, orgnr, DPE_INNSYN);
            return Optional.of(sr);
        }
        return Optional.empty();
    }

    @SuppressWarnings("squid:S1166") // Suppress Sonar due to no rethrow/log from certificate ex.
    public Optional<ServiceRecord> createDpeDataServiceRecord(String orgnr) {
        if (elmaLookupService.identifierHasInnsynDataCapability(NORWAY_PREFIX + orgnr)) {
            String pemCertificate;
            try {
                pemCertificate = lookupPemCertificate(orgnr);
            } catch (CertificateNotFoundException e) {
                log.info(MarkerFactory.receiverMarker(orgnr), String.format("Identifier %s found in ELMA with " +
                        "DPE Data profile, but certificate not found in Virksert.", orgnr));
                return Optional.empty();
            }
            DpeServiceRecord sr = DpeServiceRecord.of(pemCertificate, orgnr, DPE_DATA);
            return Optional.of(sr);
        }
        return Optional.empty();
    }

    @SuppressWarnings("squid:S1166") // Suppress Sonar due to no rethrow/log from certificate ex.
    public Optional<ServiceRecord> createDpeReceiptServiceRecord(String orgnr) {
        String pemCertificate;
        try {
            pemCertificate = lookupPemCertificate(orgnr);
        } catch (CertificateNotFoundException e) {
            log.info(MarkerFactory.receiverMarker(orgnr), String.format("Certificate for %s not found in Virksert.", orgnr));
            return Optional.empty();
        }
        DpeServiceRecord sr = DpeServiceRecord.of(pemCertificate, orgnr, DPE_RECEIPT);
        return Optional.of(sr);
    }

    public Optional<ServiceRecord> createPostVirksomhetServiceRecord(String orgnr) {
        return Optional.of(new PostVirksomhetServiceRecord(properties, orgnr));
    }

    private String lookupPemCertificate(String orgnumber) throws CertificateNotFoundException {
        try {
            return virksertService.getCertificate(orgnumber);
        } catch (VirksertClientException e) {
            throw new CertificateNotFoundException(String.format("Unable to find certificate for: %s", orgnumber), e);
        }
    }

    @PreAuthorize("#oauth2.hasScope('move/dpi.read')")
    public Optional<ServiceRecord> createServiceRecordForCititzen(String identifier,
                                                                 boolean createIfMissingMailbox,
                                                                 Authentication auth,
                                                                 String onBehalfOrgnr,
                                                                 Notification notification) throws KRRClientException {

        String token = ((OAuth2AuthenticationDetails) auth.getDetails()).getTokenValue();

        PersonResource personResource = krrService.getCizitenInfo(lookup(identifier)
                .onBehalfOf(onBehalfOrgnr)
                .require(notification)
                .token(token));

        if (!personResource.hasMailbox() &&
                NEI.name().equals(personResource.getReserved()) &&
                !createIfMissingMailbox) {
            return createPostVirksomhetServiceRecord(identifier);
        }

        Optional<DSFResource> dsfResource = krrService.getDSFInfo(identifier, token);
        if (!dsfResource.isPresent()) {
            log.error("Identifier found in KRR but not in DSF, defaulting to DPV.");
            return Optional.empty();
        }
        String[] codeArea = dsfResource.get().getPostAddress().split(" ");
        PostAddress postAddress = new PostAddress(dsfResource.get().getName(),
                dsfResource.get().getStreet(),
                codeArea[0],
                codeArea.length > 1 ? codeArea[1] : codeArea[0],
                dsfResource.get().getCountry());

        Optional<EntityInfo> senderEntity = entityService.getEntityInfo(onBehalfOrgnr);
        PostAddress returnAddress;
        if (senderEntity.isPresent() && senderEntity.get() instanceof OrganizationInfo) {
            OrganizationInfo orginfo = (OrganizationInfo) senderEntity.get();
            returnAddress = new PostAddress(orginfo.getOrganizationName(),
                    orginfo.getPostadresse().getAdresse(),
                    orginfo.getPostadresse().getPostnummer(),
                    orginfo.getPostadresse().getPoststed(),
                    orginfo.getPostadresse().getLand());
        } else {
            return Optional.empty();
        }

        return Optional.of(new SikkerDigitalPostServiceRecord(properties, personResource, ServiceIdentifier.DPI,
                identifier, postAddress, returnAddress));
    }

}

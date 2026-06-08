package no.difi.meldingsutveksling.serviceregistry.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.domain.Iso6523;
import no.difi.meldingsutveksling.domain.PartnerIdentifier;
import no.difi.meldingsutveksling.serviceregistry.CertificateNotFoundException;
import no.difi.meldingsutveksling.serviceregistry.SRRequestScope;
import no.difi.meldingsutveksling.serviceregistry.config.ServiceregistryProperties;
import no.difi.meldingsutveksling.serviceregistry.domain.CitizenInfo;
import no.difi.meldingsutveksling.serviceregistry.domain.Entity;
import no.difi.meldingsutveksling.serviceregistry.domain.EntityInfo;
import no.difi.meldingsutveksling.serviceregistry.domain.FiksIoInfo;
import no.difi.meldingsutveksling.serviceregistry.domain.HelseEnhetInfo;
import no.difi.meldingsutveksling.serviceregistry.domain.Notification;
import no.difi.meldingsutveksling.serviceregistry.domain.OrganizationInfo;
import no.difi.meldingsutveksling.serviceregistry.domain.Process;
import no.difi.meldingsutveksling.serviceregistry.domain.ProcessCategory;
import no.difi.meldingsutveksling.serviceregistry.exceptions.EntityNotFoundException;
import no.difi.meldingsutveksling.serviceregistry.exceptions.ReceiverProcessNotFoundException;
import no.difi.meldingsutveksling.serviceregistry.exceptions.SecurityLevelNotFoundException;
import no.difi.meldingsutveksling.serviceregistry.exceptions.ServiceRegistryException;
import no.difi.meldingsutveksling.serviceregistry.freg.exception.FregGatewayException;
import no.difi.meldingsutveksling.serviceregistry.krr.KontaktInfoException;
import no.difi.meldingsutveksling.serviceregistry.record.ServiceRecord;
import no.difi.meldingsutveksling.serviceregistry.record.ServiceRecordService;
import no.difi.meldingsutveksling.serviceregistry.security.EntitySignerException;
import no.difi.meldingsutveksling.serviceregistry.security.PayloadSigner;
import no.difi.meldingsutveksling.serviceregistry.service.AuthenticationService;
import no.difi.meldingsutveksling.serviceregistry.service.EntityService;
import no.difi.meldingsutveksling.serviceregistry.service.ProcessService;
import no.difi.meldingsutveksling.serviceregistry.service.brreg.BrregNotFoundException;
import no.difi.meldingsutveksling.serviceregistry.svarut.SvarUtClientException;
import no.difi.move.common.IdentifierHasher;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;

import java.util.List;
import java.util.Optional;

import static no.difi.meldingsutveksling.serviceregistry.businesslogic.ServiceRecordPredicates.shouldCreateServiceRecordForCitizen;
import static no.difi.meldingsutveksling.serviceregistry.logging.SRMarkerFactory.markerFrom;

@Slf4j
@RestController
@RequiredArgsConstructor
public class ServiceRecordController {

    static final String X_ENABLE_BETA_FEATURES = "X-Enable-Beta-Features";

    private final ObjectMapper objectMapper;
    private final EntityService entityService;
    private final PayloadSigner payloadSigner;
    private final SRRequestScope requestScope;
    private final ProcessService processService;
    private final ServiceRecordService serviceRecordService;
    private final AuthenticationService authenticationService;
    private final ServiceregistryProperties properties;

    @InitBinder
    protected void initBinders(WebDataBinder binder) {
        binder.registerCustomEditor(Notification.class, new NotificationEditor());
    }

    /**
     * Used to retrieve information needed to send a message within the provided process
     * to an entity with the provided identifier.
     *
     * @param partnerIdentifier specifies the target entity.
     * @param processIdentifier specifies the target process.
     * @param auth              provides the authentication object.
     * @param request           is the servlet request.
     * @return JSON object with information needed to send a message.
     */
    @GetMapping(value = "/identifier/{identifier}/process/{processIdentifier}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<?> entity(@PathVariable(name = "identifier") PartnerIdentifier partnerIdentifier,
                                    @PathVariable String processIdentifier,
                                    @RequestParam(name = "securityLevel", required = false) Integer securityLevel,
                                    @RequestParam(name = "conversationId", required = false) String conversationId,
                                    @RequestParam(name = "print", defaultValue = "true") boolean print,
                                    @RequestHeader(name = X_ENABLE_BETA_FEATURES, defaultValue = "false", required = false) boolean enableBetaFeatures,
                                    Authentication auth,
                                    HttpServletRequest request)
            throws SecurityLevelNotFoundException, CertificateNotFoundException, KontaktInfoException,
            BrregNotFoundException, SvarUtClientException, ReceiverProcessNotFoundException, FregGatewayException {
        String identifier = partnerIdentifier.getIdentifier();

        MDC.put("identifier", Strings.isNullOrEmpty(identifier) ? identifier : IdentifierHasher.hashIfPersonnr(identifier));
        Iso6523 clientId = authenticationService.getAuthorizedClientIdentifier(auth, request);

        fillRequestScope(identifier, conversationId, clientId, authenticationService.getToken(auth));

        EntityInfo entityInfo = entityService.getEntityInfo(partnerIdentifier)
                .orElseThrow(() -> new EntityNotFoundException(identifier));
        Entity entity = new Entity();
        entity.setInfoRecord(entityInfo);

        if (entityInfo instanceof FiksIoInfo) {
            ServiceRecord fiksIoRecord = serviceRecordService.createFiksIoServiceRecord(entityInfo, processIdentifier)
                    .orElseThrow(() -> new ReceiverProcessNotFoundException(identifier, processIdentifier));
            entity.getServiceRecords().add(fiksIoRecord);
            return ResponseEntity.ok(entity);
        }

        Process process = processService.findByIdentifier(processIdentifier).orElseThrow(() -> new ReceiverProcessNotFoundException(identifier, processIdentifier));

        if (entityInfo instanceof HelseEnhetInfo helseEnhetInfo && isHealthcareEnabled() && enableBetaFeatures) {
            List<ServiceRecord> healthcareServiceRecords = serviceRecordService.createHealthcareServiceRecords(helseEnhetInfo, process);
            entity.getServiceRecords().addAll(healthcareServiceRecords);
            return ResponseEntity.ok(entity);
        }

        if (ProcessCategory.DIGITALPOST == process.getCategory() && shouldCreateServiceRecordForCitizen().test(entityInfo)) {
            entity.getServiceRecords().addAll(serviceRecordService.createDigitalpostServiceRecords(identifier, clientId, print, process));
        }
        if (ProcessCategory.ARKIVMELDING == process.getCategory()) {
            ServiceRecord record = serviceRecordService.createArkivmeldingServiceRecord(entityInfo, process, securityLevel)
                    .orElseThrow(() -> new ReceiverProcessNotFoundException(identifier, processIdentifier));
            entity.getServiceRecords().add(record);
        }
        if (ProcessCategory.EINNSYN == process.getCategory()) {
            ServiceRecord record = serviceRecordService.createServiceRecord(entityInfo, process, securityLevel)
                    .orElseThrow(() -> new ReceiverProcessNotFoundException(identifier, processIdentifier));
            entity.getServiceRecords().add(record);
        }
        if (ProcessCategory.AVTALT == process.getCategory()) {
            ServiceRecord record = serviceRecordService.createServiceRecord(entityInfo, process, securityLevel)
                    .orElseThrow(() -> new ReceiverProcessNotFoundException(identifier, processIdentifier));
            entity.getServiceRecords().add(record);
        }
        if (entity.getServiceRecords().isEmpty()) {
            throw new ReceiverProcessNotFoundException(identifier, processIdentifier);
        }

        return ResponseEntity.ok(entity);
    }

    /**
     * Used to retrieve information needed to send a message to an entity (organization or a person) having the provided
     * identifier
     *
     * @param partnerIdentifier of the organization/person to receive a message
     * @return JSON object with information needed to send a message
     */
    @GetMapping(value = "/identifier/{identifier}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @SuppressWarnings("squid:S2583")
    public ResponseEntity<?> entity(
            @PathVariable(name = "identifier") PartnerIdentifier partnerIdentifier,
            @RequestParam(name = "securityLevel", required = false) Integer securityLevel,
            @RequestParam(name = "conversationId", required = false) String conversationId,
            @RequestParam(name = "print", defaultValue = "true") boolean print,
            @RequestHeader(name = X_ENABLE_BETA_FEATURES, defaultValue = "false", required = false) boolean enableBetaFeatures,
            Authentication auth,
            HttpServletRequest request)
            throws SecurityLevelNotFoundException, CertificateNotFoundException, KontaktInfoException, BrregNotFoundException, SvarUtClientException {

        String identifier = partnerIdentifier.getIdentifier();

        log.trace("Identifier: {}", partnerIdentifier);
        MDC.put("identifier", Strings.isNullOrEmpty(identifier) ? identifier : IdentifierHasher.hashIfPersonnr(identifier));
        Iso6523 clientOrgnr = authenticationService.getAuthorizedClientIdentifier(auth, request);
        log.trace("Client orgnr: {}", clientOrgnr);
        fillRequestScope(identifier, conversationId, clientOrgnr, authenticationService.getToken(auth));

        Entity entity = new Entity();
        EntityInfo entityInfo = entityService.getEntityInfo(partnerIdentifier)
                .orElseThrow(() -> new EntityNotFoundException(identifier));
        entity.setInfoRecord(entityInfo);

        switch (entityInfo) {
            case OrganizationInfo _ -> {
                {
                    log.trace("Organization");
                    entity.getServiceRecords().addAll(serviceRecordService.createArkivmeldingServiceRecords(entityInfo, securityLevel));
                    entity.getServiceRecords().addAll(serviceRecordService.createEinnsynServiceRecords(entityInfo, securityLevel));
                    entity.getServiceRecords().addAll(serviceRecordService.createAvtaltServiceRecords(identifier));
                }
            }
            case CitizenInfo _ -> {
                log.trace("Citizen");
                try {
                    entity.getServiceRecords().addAll(serviceRecordService.createDigitalpostServiceRecords(identifier, clientOrgnr, print));
                } catch (FregGatewayException | HttpClientErrorException e) {
                    log.info("No service record found for citizen: {}", identifier);
                    return new ResponseEntity<>("{\"message\": \"No service record found for citizen: " + identifier + "\"}", HttpStatus.NOT_FOUND);
                }
            }
            case FiksIoInfo _ -> {
            }
            case HelseEnhetInfo helseEnhetInfo -> {
                if(isHealthcareEnabled() && enableBetaFeatures) {
                    try {
                        entity.getServiceRecords().addAll(serviceRecordService.createHealthcareServiceRecords(helseEnhetInfo));
                    } catch (Exception e) {
                        log.warn("Error while retrieving Healthcare record.", e);
                    }
                }
            }
            default -> {

            }
        }

        return new ResponseEntity<>(entity, HttpStatus.OK);
    }

    private void fillRequestScope(String identifier, String conversationId, Iso6523 clientId, Jwt token) {
        requestScope.setConversationId(conversationId);
        requestScope.setIdentifier(identifier);
        Optional.ofNullable(clientId).map(Iso6523::getOrganizationIdentifier).ifPresent(requestScope::setClientId);
        requestScope.setToken(token);
    }

    private ResponseEntity<?> notFoundResponse(String logMessage) {
        log.error(markerFrom(requestScope), logMessage);
        return ResponseEntity.notFound().build();
    }

    @GetMapping(value = "/identifier/{identifier}", produces = "application/jose")
    @ResponseBody
    public ResponseEntity<?> signed(
            @PathVariable(name = "identifier") PartnerIdentifier partnerIdentifier,
            @RequestParam(name = "securityLevel", required = false) Integer securityLevel,
            @RequestParam(name = "conversationId", required = false) String conversationId,
            @RequestParam(name = "print", defaultValue = "true") boolean print,
            @RequestHeader(name = X_ENABLE_BETA_FEATURES, defaultValue = "false", required = false) boolean enableBetaFeatures,
            Authentication auth,
            HttpServletRequest request)
            throws EntitySignerException, SecurityLevelNotFoundException, KontaktInfoException,
            CertificateNotFoundException, BrregNotFoundException, SvarUtClientException, FregGatewayException {
        return signEntity(entity(partnerIdentifier, securityLevel, conversationId, print, enableBetaFeatures, auth, request));
    }

    @GetMapping(value = "/identifier/{identifier}/process/{processIdentifier}", produces = "application/jose")
    @ResponseBody
    public ResponseEntity<?> signed(@PathVariable(name = "identifier") PartnerIdentifier partnerIdentifier,
                                    @PathVariable String processIdentifier,
                                    @RequestParam(name = "securityLevel", required = false) Integer securityLevel,
                                    @RequestParam(name = "conversationId", required = false) String conversationId,
                                    @RequestParam(name = "print", defaultValue = "true") boolean print,
                                    @RequestHeader(name = X_ENABLE_BETA_FEATURES, defaultValue = "false", required = false) boolean enableBetaFeatures,
                                    Authentication auth,
                                    HttpServletRequest request)
            throws SecurityLevelNotFoundException, KontaktInfoException, CertificateNotFoundException,
            BrregNotFoundException, SvarUtClientException, EntitySignerException, ReceiverProcessNotFoundException, FregGatewayException {
        return signEntity(entity(partnerIdentifier, processIdentifier, securityLevel, conversationId, print, enableBetaFeatures, auth, request));
    }

    @GetMapping(value = "/info/{identifier}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<?> info(@PathVariable(name = "identifier") PartnerIdentifier partnerIdentifier, Authentication auth) {
        requestScope.setToken(authenticationService.getToken(auth));

        Entity entity = new Entity();
        Optional<EntityInfo> entityInfo = entityService.getEntityInfo(partnerIdentifier);
        if (entityInfo.isEmpty()) {
            return notFoundResponse(String.format("Entity with identifier '%s' not found.", partnerIdentifier));
        }
        entity.setInfoRecord(entityInfo.get());
        return ResponseEntity.ok(entity);
    }

    @GetMapping(value = "/info/{identifier}", produces = "application/jose")
    @ResponseBody
    public ResponseEntity<?> signed(@PathVariable(name = "identifier") PartnerIdentifier partnerIdentifier, Authentication auth) throws EntitySignerException {
        return signEntity(info(partnerIdentifier, auth));
    }

    private ResponseEntity<?> signEntity(ResponseEntity<?> entity) throws EntitySignerException {
        if (entity.getStatusCode() != HttpStatus.OK) {
            log.info("Entity status code is {}, skipping signing", entity.getStatusCode());
            return entity;
        }
        String json;
        try {
            json = objectMapper.writeValueAsString(entity.getBody());
        } catch (JsonProcessingException e) {
            log.error(markerFrom(requestScope), "Failed to convert entity to json", e);
            throw new ServiceRegistryException(e);
        }

        return ResponseEntity.ok(payloadSigner.sign(json));
    }

    private boolean isHealthcareEnabled() {
        return properties.getHealthcare().isEnabled();
    }
}
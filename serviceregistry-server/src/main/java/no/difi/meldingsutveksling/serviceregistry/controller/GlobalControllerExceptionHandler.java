package no.difi.meldingsutveksling.serviceregistry.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.serviceregistry.CertificateNotFoundException;
import no.difi.meldingsutveksling.serviceregistry.ErrorResponse;
import no.difi.meldingsutveksling.serviceregistry.SRRequestScope;
import no.difi.meldingsutveksling.serviceregistry.exceptions.*;
import no.difi.meldingsutveksling.serviceregistry.krr.KontaktInfoException;
import no.difi.meldingsutveksling.serviceregistry.logging.SRMarkerFactory;
import no.difi.meldingsutveksling.serviceregistry.mvc.UnknownServiceIdentifierException;
import no.difi.meldingsutveksling.serviceregistry.service.brreg.BrregNotFoundException;
import no.difi.meldingsutveksling.serviceregistry.svarut.SvarUtClientException;
import no.difi.virksert.client.lang.VirksertClientException;
import no.difi.meldingsutveksling.serviceregistry.service.healthcare.PatientNotRetrievedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@Slf4j
@ControllerAdvice
@RequiredArgsConstructor
public class GlobalControllerExceptionHandler {

    private final SRRequestScope requestScope;
    private final ObjectMapper om = new ObjectMapper();

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<?> entityNotFound(HttpServletRequest request, Exception e) {
        log.debug(SRMarkerFactory.markerFrom(requestScope), "Entity not found for {}", request.getRequestURL(), e);
        return errorResponse(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<?> accessDenied(HttpServletRequest request, Exception e) {
        log.warn(SRMarkerFactory.markerFrom(requestScope), "Access denied on resource " + request.getRequestURL(), e);
        return errorResponse(HttpStatus.UNAUTHORIZED, e.getMessage());
    }

    @ExceptionHandler(SecurityLevelNotFoundException.class)
    public ResponseEntity<?> securityLevelNotFound(HttpServletRequest request, Exception e) {
        log.warn(SRMarkerFactory.markerFrom(requestScope), "Security level not found for " + request.getRequestURL(), e);
        return errorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(ProcessNotFoundException.class)
    public ResponseEntity<?> processNotFound(HttpServletRequest request, Exception e) {
        log.error(SRMarkerFactory.markerFrom(requestScope), "Exception occured on " + request.getRequestURL(), e);
        return errorResponse(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(ReceiverProcessNotFoundException.class)
    public ResponseEntity<?> receiverProcessNotFound(HttpServletRequest request, Exception e) {
        log.warn(SRMarkerFactory.markerFrom(requestScope), "Exception occured on " + request.getRequestURL() + " - " + e.getMessage());
        return errorResponse(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(KontaktInfoException.class)
    public ResponseEntity<?> krrClientException(HttpServletRequest request, Exception e) {
        log.error(SRMarkerFactory.markerFrom(requestScope), "Exception occurred on " + request.getRequestURL(), e);
        return errorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(SvarUtClientException.class)
    public ResponseEntity<?> handleSvarUtClientException(HttpServletRequest request, Exception e) {
        log.error(SRMarkerFactory.markerFrom(requestScope), "Exception occurred on " + request.getRequestURL(), e);
        return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
    }

    @ExceptionHandler(NonMatchingCertificatesException.class)
    public ResponseEntity<?> nonMatchingCertificate(HttpServletRequest request, Exception e) {
        log.warn(SRMarkerFactory.markerFrom(requestScope), "Certificate request error on " + request.getRequestURL() + " - " + e.getMessage());
        return errorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(CertificateNotFoundException.class)
    public ResponseEntity<?> certificateNotFound(HttpServletRequest request, Exception e) {
        log.warn(SRMarkerFactory.markerFrom(requestScope), "Certificate not found for " + request.getRequestURL());
        return errorResponse(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(VirksertClientException.class)
    public ResponseEntity<?> virksertError(HttpServletRequest request, Exception e) {
        log.warn(SRMarkerFactory.markerFrom(requestScope), "Virksert lookup failed for " + request.getRequestURL() + " -> " + e.getMessage());
        return errorResponse(HttpStatus.NOT_FOUND, "Virksert lookup failed - " + e.getMessage(), "certificate_not_found");
    }

    @ExceptionHandler(BrregNotFoundException.class)
    public ResponseEntity<?> brregNotFoundException(HttpServletRequest request, Exception e) {
        log.warn(SRMarkerFactory.markerFrom(requestScope), "BRREG lookup failed for " + request.getRequestURL(), e);
        return errorResponse(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(UnknownServiceIdentifierException.class)
    public ResponseEntity<?> unknownServiceIdentifie(HttpServletRequest request, Exception e) {
        log.debug(SRMarkerFactory.markerFrom(requestScope), "Converting service identifier failed for " + request.getRequestURL(), e);
        return errorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(ClientInputException.class)
    public ResponseEntity<?> clientInputException(HttpServletRequest request, Exception e) {
        log.error(SRMarkerFactory.markerFrom(requestScope), "Client input error", e);
        return errorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(PatientNotRetrievedException.class)
    public ResponseEntity<?> pationInformationNotFound(HttpServletRequest request, Exception e) {
        log.error(SRMarkerFactory.markerFrom(requestScope), "Not able to load Patient information", e);
        return errorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    private ResponseEntity<?> errorResponse(HttpStatus status, String msg) {
        return errorResponse(status, msg, "");
    }

    private ResponseEntity<?> errorResponse(HttpStatus status, String msg, String code) {
        try {
            return ResponseEntity.status(status)
                .body(om.writeValueAsString(ErrorResponse.builder()
                        .errorDescription(msg)
                        .errorCode(code)
                        .build()));
        } catch (JsonProcessingException ex) {
            log.error(ex.getMessage(), ex);
            return (ResponseEntity<?>) ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
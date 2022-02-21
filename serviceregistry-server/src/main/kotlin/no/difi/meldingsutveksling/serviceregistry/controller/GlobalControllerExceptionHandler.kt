package no.difi.meldingsutveksling.serviceregistry.controller

import com.fasterxml.jackson.databind.ObjectMapper
import no.difi.meldingsutveksling.serviceregistry.CertificateNotFoundException
import no.difi.meldingsutveksling.serviceregistry.ErrorResponse
import no.difi.meldingsutveksling.serviceregistry.SRRequestScope
import no.difi.meldingsutveksling.serviceregistry.exceptions.*
import no.difi.meldingsutveksling.serviceregistry.krr.KontaktInfoException
import no.difi.meldingsutveksling.serviceregistry.logger
import no.difi.meldingsutveksling.serviceregistry.logging.SRMarkerFactory.markerFrom
import no.difi.meldingsutveksling.serviceregistry.mvc.UnknownServiceIdentifierException
import no.difi.meldingsutveksling.serviceregistry.service.brreg.BrregNotFoundException
import no.difi.meldingsutveksling.serviceregistry.svarut.SvarUtClientException
import no.difi.virksert.client.lang.VirksertClientException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import javax.servlet.http.HttpServletRequest

@ControllerAdvice
class GlobalControllerExceptionHandler(private val requestScope: SRRequestScope) {

    private val log = logger()
    private val om = ObjectMapper()

    @ExceptionHandler(EntityNotFoundException::class)
    fun entityNotFound(request: HttpServletRequest, e: Exception): ResponseEntity<*> {
        log.debug(markerFrom(requestScope), "Entity not found for {}", request.requestURL, e)
        return errorResponse(HttpStatus.NOT_FOUND, e.message)
    }

    @ExceptionHandler(AccessDeniedException::class)
    fun accessDenied(request: HttpServletRequest, e: Exception): ResponseEntity<*> {
        log.warn(markerFrom(requestScope), "Access denied on resource ${request.requestURL}", e)
        return errorResponse(HttpStatus.UNAUTHORIZED, e.message)
    }

    @ExceptionHandler(SecurityLevelNotFoundException::class)
    fun securityLevelNotFound(request: HttpServletRequest, e: Exception): ResponseEntity<*> {
        log.warn(markerFrom(requestScope), "Security level not found for ${request.requestURL}", e)
        return errorResponse(HttpStatus.BAD_REQUEST, e.message)
    }

    @ExceptionHandler(ProcessNotFoundException::class)
    fun processNotFound(request: HttpServletRequest, e: Exception): ResponseEntity<*> {
        log.error(markerFrom(requestScope), "Exception occured on ${request.requestURL}", e)
        return errorResponse(HttpStatus.NOT_FOUND, e.message)
    }

    @ExceptionHandler(ReceiverProcessNotFoundException::class)
    fun receiverProcessNotFound(request: HttpServletRequest, e: Exception): ResponseEntity<*> {
        log.warn(markerFrom(requestScope), "Exception occured on ${request.requestURL} - ${e.message}")
        return errorResponse(HttpStatus.NOT_FOUND, e.message)
    }

    @ExceptionHandler(KontaktInfoException::class)
    fun krrClientException(request: HttpServletRequest, e: Exception): ResponseEntity<*> {
        log.error(markerFrom(requestScope), "Exception occurred on ${request.requestURL}", e)
        return errorResponse(HttpStatus.BAD_REQUEST, e.message)
    }

    @ExceptionHandler(SvarUtClientException::class)
    fun handleSvarUtClientException(request: HttpServletRequest, e: Exception): ResponseEntity<*> {
        log.error(markerFrom(requestScope), "Exception occurred on ${request.requestURL}", e)
        return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.message)
    }

    @ExceptionHandler(NonMatchingCertificatesException::class)
    fun nonMatchingCertificate(request: HttpServletRequest, e: Exception): ResponseEntity<*> {
        log.warn(markerFrom(requestScope), "Certificate request error on ${request.requestURL} - ${e.message}")
        return errorResponse(HttpStatus.BAD_REQUEST, e.message)
    }

    @ExceptionHandler(CertificateNotFoundException::class)
    fun certificateNotFound(request: HttpServletRequest, e: Exception): ResponseEntity<*> {
        log.warn(markerFrom(requestScope), "Certificate not found for ${request.requestURL}")
        return errorResponse(HttpStatus.NOT_FOUND, e.message)
    }

    @ExceptionHandler(VirksertClientException::class)
    fun virksertError(request: HttpServletRequest, e: Exception): ResponseEntity<*> {
        log.warn(markerFrom(requestScope), "Virksert lookup failed for ${request.requestURL} -> ${e.message}")
        return errorResponse(HttpStatus.NOT_FOUND, "Virksert lookup failed - ${e.message}", "certificate_not_found")
    }

    @ExceptionHandler(BrregNotFoundException::class)
    fun brregNotFoundException(request: HttpServletRequest, e: Exception): ResponseEntity<*> {
        log.warn(markerFrom(requestScope), "BRREG lookup failed for ${request.requestURL}", e)
        return errorResponse(HttpStatus.NOT_FOUND, e.message)
    }

    @ExceptionHandler(UnknownServiceIdentifierException::class)
    fun unknownServiceIdentifie(request: HttpServletRequest, e: Exception): ResponseEntity<*> {
        log.debug(markerFrom(requestScope), "Converting service identifier failed for ${request.requestURL}", e)
        return errorResponse(HttpStatus.BAD_REQUEST, e.message)
    }

    private fun errorResponse(status: HttpStatus, msg: String?, code: String = ""): ResponseEntity<*> {
        return ResponseEntity.status(status)
            .body(om.writeValueAsString(ErrorResponse.builder()
                                            .errorDescription(msg)
                                            .errorCode(code)
                                            .build()))
    }
}
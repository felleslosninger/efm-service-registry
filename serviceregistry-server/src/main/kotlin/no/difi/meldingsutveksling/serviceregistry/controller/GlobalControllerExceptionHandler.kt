package no.difi.meldingsutveksling.serviceregistry.controller

import no.difi.meldingsutveksling.serviceregistry.CertificateNotFoundException
import no.difi.meldingsutveksling.serviceregistry.ErrorResponse
import no.difi.meldingsutveksling.serviceregistry.SRRequestScope
import no.difi.meldingsutveksling.serviceregistry.exceptions.EntityNotFoundException
import no.difi.meldingsutveksling.serviceregistry.exceptions.ProcessNotFoundException
import no.difi.meldingsutveksling.serviceregistry.exceptions.SecurityLevelNotFoundException
import no.difi.meldingsutveksling.serviceregistry.krr.KontaktInfoException
import no.difi.meldingsutveksling.serviceregistry.logger
import no.difi.meldingsutveksling.serviceregistry.logging.SRMarkerFactory.markerFrom
import no.difi.meldingsutveksling.serviceregistry.service.brreg.BrregNotFoundException
import no.difi.meldingsutveksling.serviceregistry.svarut.SvarUtClientException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import javax.servlet.http.HttpServletRequest

@ControllerAdvice
class GlobalControllerExceptionHandler(val requestScope: SRRequestScope) {

    val log = logger()

    @ExceptionHandler(EntityNotFoundException::class)
    fun entityNotFound(request: HttpServletRequest, e: Exception): ResponseEntity<ErrorResponse> {
        log.warn(markerFrom(requestScope), "Entity not found for {}", request.requestURL, e)
        return errorResponse(HttpStatus.NOT_FOUND, e.message)
    }

    @ExceptionHandler(AccessDeniedException::class)
    fun accessDenied(request: HttpServletRequest, e: Exception): ResponseEntity<ErrorResponse> {
        log.warn(markerFrom(requestScope), "Access denied on resource ${request.requestURL}", e)
        return errorResponse(HttpStatus.UNAUTHORIZED, e.message)
    }

    @ExceptionHandler(SecurityLevelNotFoundException::class)
    fun securityLevelNotFound(request: HttpServletRequest, e: Exception): ResponseEntity<ErrorResponse> {
        log.warn(markerFrom(requestScope), "Security level not found for ${request.requestURL}", e)
        return errorResponse(HttpStatus.BAD_REQUEST, e.message)
    }

    @ExceptionHandler(ProcessNotFoundException::class)
    fun processNotFound(request: HttpServletRequest, e: Exception): ResponseEntity<ErrorResponse> {
        log.error(markerFrom(requestScope), "Exception occured on ${request.requestURL}", e)
        return errorResponse(HttpStatus.NOT_FOUND, e.message)
    }

    @ExceptionHandler(KontaktInfoException::class)
    fun krrClientException(request: HttpServletRequest, e: Exception): ResponseEntity<ErrorResponse> {
        log.error(markerFrom(requestScope), "Exception occurred on ${request.requestURL}", e)
        return errorResponse(HttpStatus.BAD_REQUEST, e.message)
    }

    @ExceptionHandler(SvarUtClientException::class)
    fun handleSvarUtClientException(request: HttpServletRequest, e: Exception): ResponseEntity<ErrorResponse> {
        log.error(markerFrom(requestScope), "Exception occurred on ${request.requestURL}", e)
        return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.message)
    }

    @ExceptionHandler(CertificateNotFoundException::class)
    fun certificateNotFound(request: HttpServletRequest, e: Exception): ResponseEntity<ErrorResponse> {
        log.warn(markerFrom(requestScope), "Certificate not found for ${request.requestURL}", e)
        return errorResponse(HttpStatus.BAD_REQUEST, e.message)
    }

    @ExceptionHandler(BrregNotFoundException::class)
    fun brregNotFoundException(request: HttpServletRequest, e: Exception): ResponseEntity<ErrorResponse> {
        log.warn(markerFrom(requestScope), "BRREG lookup failed for ${request.requestURL}", e)
        return errorResponse(HttpStatus.NOT_FOUND, e.message)
    }

    private fun errorResponse(status: HttpStatus, msg: String?, code: String = ""): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(status)
                .body(ErrorResponse.builder()
                        .errorDescription(msg)
                        .errorCode(code)
                        .build())
    }
}
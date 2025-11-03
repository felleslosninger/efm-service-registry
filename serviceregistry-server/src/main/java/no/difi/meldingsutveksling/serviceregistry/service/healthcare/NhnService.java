package no.difi.meldingsutveksling.serviceregistry.service.healthcare;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.serviceregistry.exceptions.ClientInputException;
import no.difi.meldingsutveksling.serviceregistry.exceptions.EntityNotFoundException;
import no.difi.meldingsutveksling.serviceregistry.exceptions.ServiceRegistryException;
import no.difi.meldingsutveksling.serviceregistry.record.LookupParameters;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@AllArgsConstructor
@Slf4j
public class NhnService {


    private String uri;
    private final RestClient restClient;

    public AddressRegistrerDetails getARDetails(LookupParameters param) {

        try {
            return restClient.get()
                    .uri(uri, param.getIdentifier())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + param.getToken().getTokenValue())
                    .header(HttpHeaders.ACCEPT, "application/json")
                    .retrieve()
                    .onStatus( status -> status.equals(HttpStatus.NOT_FOUND),  (request, response) -> {
                        throw new EntityNotFoundException(
                                param.getIdentifier());
                    })
                    .onStatus( status -> status.equals(HttpStatus.UNAUTHORIZED),  (request, response) -> {
                        throw new AccessDeniedException("AR lookup Access denied, identifier " + param.getIdentifier());
                    })
                    .onStatus(HttpStatusCode::is4xxClientError,  (request, response) -> {
                        throw new ClientInputException("Client input error for identifier" + param.getIdentifier());
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (request,response) ->{ throw new ServiceRegistryException(
                    "Internal server fail while getting AR details for id " + param.getIdentifier());}).toEntity(AddressRegistrerDetails.class).getBody();

        }catch (ResourceAccessException e) {
            log.warn("Healthcare servcie is down");
            throw e;
        }
        catch (RestClientException e) {
            throw new ServiceRegistryException(
                    "Error fetching AR details for identifier: %s".formatted(param.getIdentifier()), e);
        }
    }
    }










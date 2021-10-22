package no.difi.meldingsutveksling.serviceregistry.krr;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.serviceregistry.config.ServiceregistryProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Profile("production")
public class DefaultDsfClient extends KontaktInfoClient implements DsfClient {

    private final ObjectMapper objectMapper;
    private final ServiceregistryProperties properties;

    @Override
    public Optional<DsfResource> getDSFResource(LookupParameters params, URI endpointUri) throws KontaktInfoException {

        String response = fetchKontaktInfo(params.getIdentifier(), params.getToken().getTokenValue(), endpointUri);

        if (params.getToken().getIssuer().toString().equals(properties.getAuth().getOidcIssuer())) {
            return mapResponse(response, DsfResource.class);
        } else {
            Optional<DsfMpResource> mpResource = mapResponse(response, DsfMpResource.class);
            return mpResource.map(r -> DsfResource.builder()
                .personIdentifier(r.getPersonIdentifier())
                .name(r.getNavn().getForkortetNavn())
                .street(String.join(";", r.getPostadresse().getAdresselinje()))
                .postAddress(r.getPostadresse().getPostnummer() + " " + r.getPostadresse().getPoststed())
                .country(r.getPostadresse().getLandkode())
                .build());
        }
    }

    private <T> Optional<T> mapResponse(String response, Class<T> clazz) throws KontaktInfoException {
        DsfResponse<T> mappedResponse;
        try {
            mappedResponse = objectMapper.readValue(response,
                objectMapper.getTypeFactory().constructParametricType(DsfResponse.class, clazz));

        } catch (IOException e) {
            throw new KontaktInfoException("Error mapping payload to " + DsfResponse.class.getName(), e);
        }

        if (mappedResponse.getPersonList() == null || mappedResponse.getPersonList().isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(mappedResponse.getPersonList().get(0));
    }

}

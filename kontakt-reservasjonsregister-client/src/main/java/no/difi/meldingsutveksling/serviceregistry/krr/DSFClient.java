package no.difi.meldingsutveksling.serviceregistry.krr;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class DSFClient extends KontaktInfoClient {

    private final ObjectMapper objectMapper;

    public Optional<DsfResource> getDSFResource(LookupParameters params, URI endpointUri, String oidcTokenIssuer) throws KontaktInfoException {

        String response = fetchKontaktInfo(params.getIdentifier(), params.getToken().getTokenValue(), endpointUri);

        if (params.getToken().getIssuer().toString().equals(oidcTokenIssuer)) {
            DsfOidcResponse dsfOidcResponse;
            try {
                dsfOidcResponse = objectMapper.readValue(response, DsfOidcResponse.class);
            } catch (IOException e) {
                throw new KontaktInfoException("Error mapping payload to " + DsfOidcResponse.class.getName(), e);
            }

            if (dsfOidcResponse.getPersonList() == null || dsfOidcResponse.getPersonList().isEmpty()) {
                return Optional.empty();
            }

            return Optional.of(dsfOidcResponse.getPersonList().get(0));
        } else {
            DsfMpResponse dsfMpResponse;
            try {
                dsfMpResponse = objectMapper.readValue(response, DsfMpResponse.class);
            } catch (IOException e) {
                throw new KontaktInfoException("Error mapping payload to " + DsfMpResponse.class.getName(), e);
            }

            if (dsfMpResponse.getPersonList() == null || dsfMpResponse.getPersonList().isEmpty()) {
                return Optional.empty();
            }

            DsfMpResource resource = dsfMpResponse.getPersonList().get(0);
            return Optional.of(DsfResource.builder()
                .personIdentifier(resource.getPersonIdentifier())
                .name(resource.getNavn().getForkortetNavn())
                .street(String.join(",", resource.getPostadresse().getAdresselinje()))
                .postAddress(resource.getPostadresse().getPostnummer()+" "+resource.getPostadresse().getPoststed())
                .country(resource.getPostadresse().getLandkode())
                .build());
        }
    }

}

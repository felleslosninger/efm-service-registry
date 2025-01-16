package no.difi.meldingsutveksling.serviceregistry.svarut;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.serviceregistry.config.ServiceregistryProperties;
import org.springframework.stereotype.Component;
import org.springframework.ws.client.core.WebServiceTemplate;

import jakarta.xml.bind.JAXBElement;

@Slf4j
@Component
@RequiredArgsConstructor
public class SvarUtClient {

    private final WebServiceTemplate wsTemplate;
    private final ServiceregistryProperties props;

    @SuppressWarnings("unchecked")
    public RetrieveMottakerSystemForOrgnrResponse retrieveMottakerSystemForOrgnr(RetrieveMottakerSystemForOrgnr payload) throws SvarUtClientException {
        String url = props.getFiks().getSvarut().getForsendelsesserviceUrl().toString();
        try {
            JAXBElement<RetrieveMottakerSystemForOrgnrResponse> response = (JAXBElement<RetrieveMottakerSystemForOrgnrResponse>) wsTemplate.marshalSendAndReceive(url, payload);
            return response.getValue();
        } catch (Exception e) {
            throw new SvarUtClientException(e);
        }
    }
}

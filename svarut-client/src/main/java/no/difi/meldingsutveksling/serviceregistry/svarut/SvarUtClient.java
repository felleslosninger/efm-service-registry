package no.difi.meldingsutveksling.serviceregistry.svarut;

import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.serviceregistry.config.ServiceregistryProperties;
import no.difi.meldingsutveksling.serviceregistry.lang.ExternalServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.ws.client.core.WebServiceTemplate;

import javax.xml.bind.JAXBElement;

@Component
@Slf4j
public class SvarUtClient {

    private WebServiceTemplate wsTemplate;
    private ServiceregistryProperties props;

    @Autowired
    SvarUtClient(WebServiceTemplate wsTemplate, ServiceregistryProperties props) {
        this.wsTemplate = wsTemplate;
        this.props = props;
    }

    public RetrieveMottakerSystemForOrgnrResponse retrieveMottakerSystemForOrgnr(RetrieveMottakerSystemForOrgnr payload) throws ExternalServiceException {
        String url = props.getSvarut().getForsendelsesserviceUrl().toString();
        try {
            JAXBElement<RetrieveMottakerSystemForOrgnrResponse> response = (JAXBElement<RetrieveMottakerSystemForOrgnrResponse>) wsTemplate.marshalSendAndReceive(url, payload);
            return response.getValue();
        } catch (Exception e) {
            throw new ExternalServiceException(e);
        }
    }

}

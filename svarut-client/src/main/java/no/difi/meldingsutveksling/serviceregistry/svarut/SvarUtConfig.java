package no.difi.meldingsutveksling.serviceregistry.svarut;

import net.logstash.logback.marker.Markers;
import no.difi.meldingsutveksling.serviceregistry.config.ServiceregistryProperties;
import no.difi.webservice.support.SoapFaultInterceptorLogger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.client.support.interceptor.ClientInterceptor;
import org.springframework.ws.soap.SoapMessageFactory;
import org.springframework.ws.soap.SoapVersion;
import org.springframework.ws.soap.axiom.AxiomSoapMessageFactory;
import org.springframework.ws.transport.http.AbstractHttpWebServiceMessageSender;

@Configuration
public class SvarUtConfig {

    @Bean
    public AbstractHttpWebServiceMessageSender svarUtMessageSender(ServiceregistryProperties properties) {
        return new PreauthMessageSender(
                properties.getSvarut().getUser(),
                properties.getSvarut().getPassword());
    }

    @Bean
    SoapMessageFactory messageFactory() {
        AxiomSoapMessageFactory messageFactory = new AxiomSoapMessageFactory();
        messageFactory.setSoapVersion(SoapVersion.SOAP_12);
        return messageFactory;
    }

    private Jaxb2Marshaller marshaller() {
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setContextPaths(RetrieveMottakerSystemForOrgnr.class.getPackage().getName(),
                RetrieveMottakerSystemForOrgnrResponse.class.getPackage().getName());
        return marshaller;
    }

    @Bean
    WebServiceTemplate webServiceTemplate(ServiceregistryProperties props,
            SoapMessageFactory messageFactory) {
        WebServiceTemplate template = new WebServiceTemplate();

        template.setMarshaller(marshaller());
        template.setUnmarshaller(marshaller());
        template.setMessageSender(svarUtMessageSender(props));
        template.setMessageFactory(messageFactory);

        final ClientInterceptor[] interceptors = new ClientInterceptor[1];
        interceptors[0] = SoapFaultInterceptorLogger.withLogMarkers(Markers.append("component", "svarUtClient"));
        template.setInterceptors(interceptors);

        return template;
    }
}

package no.difi.meldingsutveksling.ptp;

import net.logstash.logback.marker.Markers;
import no.difi.meldingsutveksling.serviceregistry.krr.LookupParameters;
import no.difi.ptp.sikkerdigitalpost.HentPersonerForespoersel;
import no.difi.ptp.sikkerdigitalpost.HentPersonerRespons;
import no.difi.ptp.sikkerdigitalpost.HentPrintSertifikatForespoersel;
import no.difi.ptp.sikkerdigitalpost.HentPrintSertifikatRespons;
import no.difi.ptp.sikkerdigitalpost.Informasjonsbehov;
import no.difi.ptp.sikkerdigitalpost.Oppslagstjenesten;
import no.difi.webservice.support.SoapFaultInterceptorLogger;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.Merlin;
import org.apache.wss4j.common.ext.WSPasswordCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.util.FileCopyUtils;
import org.springframework.ws.WebServiceMessage;
import org.springframework.ws.client.core.WebServiceMessageCallback;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.client.support.interceptor.ClientInterceptor;
import org.springframework.ws.soap.SoapHeader;
import org.springframework.ws.soap.SoapMessage;
import org.springframework.ws.soap.SoapVersion;
import org.springframework.ws.soap.axiom.AxiomSoapMessageFactory;
import org.springframework.ws.soap.security.wss4j2.Wss4jSecurityInterceptor;
import org.springframework.ws.soap.security.wss4j2.support.CryptoFactoryBean;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.util.JAXBSource;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;

public class OppslagstjenesteClient {

    private Configuration conf;
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass().getName());

    public OppslagstjenesteClient(Configuration configuration) {
        this.conf = configuration;
    }

    /**
     * Brukes til å hente parametre til sikker digital post forsendelse
     *
     * @param lookupParameters parametre til KRR oppslaget, dvs. personnummeret og virksomhetens organisasjonsnummer
     * @return parametre for å sende sikker digital post
     */
    public KontaktInfo hentKontaktInformasjon(LookupParameters lookupParameters) {
        final HentPersonerForespoersel hentPersonerForespoersel = HentPersonerForespoersel.builder()
                .addInformasjonsbehov(Informasjonsbehov.KONTAKTINFO, Informasjonsbehov.SIKKER_DIGITAL_POST, Informasjonsbehov.SERTIFIKAT, Informasjonsbehov.VARSLINGS_STATUS)
                .addPersonidentifikator(lookupParameters.getIdentifier())
                .build();

        WebServiceTemplate template = createWebServiceTemplate(HentPersonerRespons.class.getPackage().getName());

        WebServiceMessageCallback callback;
        if (conf.isPaaVegneAvEnabled()) {
            callback = new SoapHeaderCallbackHandler(lookupParameters.getClientOrgnr()); // must be added in production
        } else {
            callback = noopCallback -> {}; // only to be used in test/development.
        }
        final HentPersonerRespons hentPersonerRespons = (HentPersonerRespons) template.marshalSendAndReceive(conf.url, hentPersonerForespoersel, callback);

        return KontaktInfo.from(hentPersonerRespons);

    }

    /**
     * Tjenesten kan brukes for å sende forsendelser av brev til mottakere som har reservert seg eller ikke registrert/oppdatert sin kontaktinformasjon.
     *
     * @param clientOrgnr inneholder organisasjonsnummeret til virksomheten man gjør oppslag på vegne av
     * @return parametre for å sende melding til printtjenesten
     */
    public PrintProviderDetails getPrintProviderDetails(String clientOrgnr) {
        HentPrintSertifikatForespoersel request = new HentPrintSertifikatForespoersel();
        WebServiceTemplate template = createWebServiceTemplate(HentPrintSertifikatRespons.class.getPackage().getName());
        WebServiceMessageCallback callback;
        if (conf.isPaaVegneAvEnabled()) {
            callback = new SoapHeaderCallbackHandler(clientOrgnr);
        } else {
            callback = noopCallback -> {}; // only needed during development/test.
        }
        HentPrintSertifikatRespons response = (HentPrintSertifikatRespons) template.marshalSendAndReceive(conf.url, request, callback);

        return PrintProviderDetails.from(response);
    }

    /**
     * Callback to be used with Spring WS template to add PaaVegneAv element to SOAP header
     */
    private static class SoapHeaderCallbackHandler implements WebServiceMessageCallback{
        private final String orgnumber;

        private SoapHeaderCallbackHandler(String orgnumber) {
            this.orgnumber = orgnumber;
        }

        @Override
        public void doWithMessage(WebServiceMessage message) throws IOException, TransformerException {
            Oppslagstjenesten oppslagstjenesten = new Oppslagstjenesten();
            oppslagstjenesten.setPaaVegneAv(orgnumber);
            SoapMessage soapMessage = (SoapMessage) message;
            SoapHeader soapHeader = soapMessage.getSoapHeader();
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            final Transformer transformer = transformerFactory.newTransformer();
            try {
                JAXBSource jaxbSource = new JAXBSource(JAXBContext.newInstance(Oppslagstjenesten.class.getPackage().getName()), oppslagstjenesten);
                transformer.transform(jaxbSource, soapHeader.getResult());
            } catch (JAXBException e) {
                String m = String.format("Failed to add paa vegne av oppslag for %s", orgnumber);
                throw new OppslagstjenesteException(m, e);
            }
        }
    }

    private WebServiceTemplate createWebServiceTemplate(String contextPath) {
        AxiomSoapMessageFactory messageFactory = new AxiomSoapMessageFactory();
        messageFactory.setSoapVersion(SoapVersion.SOAP_12);
        WebServiceTemplate template = new WebServiceTemplate(messageFactory);

        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setContextPath(contextPath);
        template.setMarshaller(marshaller);
        template.setUnmarshaller(marshaller);
        final ClientInterceptor[] interceptors = new ClientInterceptor[2];
        interceptors[0] = createSecurityInterceptor();
        interceptors[1] = SoapFaultInterceptorLogger.withLogMarkers(Markers.append("some marker", ""));

        template.setInterceptors(interceptors);
        return template;
    }

    private ClientInterceptor createSecurityInterceptor() {
        Wss4jSecurityInterceptor securityInterceptor = new Wss4jSecurityInterceptor();
        securityInterceptor.setSecurementActions("Signature Timestamp");
        securityInterceptor.setFutureTimeToLive(120);
        securityInterceptor.setSecurementSignatureKeyIdentifier("DirectReference");
        securityInterceptor.setSecurementUsername(conf.clientAlias);
        securityInterceptor.setSecurementPassword(conf.password);
        securityInterceptor.setValidationActions("Signature Timestamp Encrypt");

        securityInterceptor.setValidationCallbackHandler(
                callbacks -> Arrays.stream(callbacks)
                        .filter(c -> c instanceof WSPasswordCallback)
                        .forEach(c -> ((WSPasswordCallback) c).setPassword(conf.password))
        );

        securityInterceptor.setSecurementSignatureAlgorithm("http://www.w3.org/2001/04/xmldsig-more#rsa-sha256");
        securityInterceptor.setSecurementSignatureParts("{}{http://www.w3.org/2003/05/soap-envelope}Body;{}{http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd}Timestamp}");
        final Crypto clientCrypto = getCryptoFactoryBean(conf.getClientJksLocation(), conf.password, conf.clientAlias);
        final Crypto serverCrypto = getCryptoFactoryBean(conf.getServerJksLocation(), conf.password, conf.serverAlias);
        securityInterceptor.setSecurementSignatureCrypto(clientCrypto);
        securityInterceptor.setValidationSignatureCrypto(serverCrypto);
        securityInterceptor.setValidationDecryptionCrypto(clientCrypto);
        securityInterceptor.setSecurementEncryptionCrypto(clientCrypto);

        return securityInterceptor;
    }

    private Crypto getCryptoFactoryBean(Resource keystore, String password, String alias) {
        final CryptoFactoryBean cryptoFactoryBean = new CryptoFactoryBean();
        try {
            cryptoFactoryBean.setKeyStoreLocation(keystore);
            cryptoFactoryBean.setKeyStoreType("jks");
            cryptoFactoryBean.setCryptoProvider(Merlin.class);
            cryptoFactoryBean.setKeyStorePassword(password);
            cryptoFactoryBean.setDefaultX509Alias(alias);
        } catch (IOException e) {
            throw new OppslagstjenesteException("Unable to create security interceptor due to problems with keystore file...", e);
        }
        try {
            cryptoFactoryBean.afterPropertiesSet();
        } catch (Exception e) {
            throw new OppslagstjenesteException("ERROR", e);
        }
        try {
            return cryptoFactoryBean.getObject();
        } catch (Exception e) {
            throw new OppslagstjenesteException("Could not create CryptoFactoryBean", e);
        }

    }

    /**
     * Parameter object to contain configuration needed to invoke the service oppslagstjeneste
     */
    public static class Configuration {

        private final String url;
        private final String password;
        private final String clientAlias;
        private final String serverAlias;
        private Resource clientJksLocation;
        private Resource serverJksLocation;
        private boolean paaVegneAvEnabled;

        /**
         * Needed to construct OppslagstjenesteClient
         *
         * @param url Url to the Oppslagstjeneste endpoint
         * @param password password for the JKS file
         * @param clientAlias for the JKS entry
         * @param clientJksLocation path to the jks file that contain the client certificate
         * @param serverJksLocation path to the jks file that contain the server certificate
         */
        public Configuration(String url, String password, String clientAlias, String serverAlias, Resource clientJksLocation, Resource serverJksLocation) {
            this.url = url;
            this.password = password;
            this.clientAlias = clientAlias;
            this.serverAlias = serverAlias;
            this.clientJksLocation = clientJksLocation;
            this.serverJksLocation = serverJksLocation;
        }

        Resource getClientJksLocation() {
            if (clientJksLocation instanceof FileSystemResource) {
                return clientJksLocation;
            }
            try {
                File tmp = File.createTempFile("difi-move", "jks");
                try (BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(tmp))) {
                    tmp.deleteOnExit();
                    FileCopyUtils.copy(clientJksLocation.getInputStream(), output);
                }
                return new FileSystemResource(tmp);
            } catch (IOException ex) {
                logger.error("Can't read keystore", ex);
            }
            return null;
        }

        Resource getServerJksLocation() {
            if (serverJksLocation instanceof FileSystemResource) {
                return serverJksLocation;
            }
            try {
                File tmp = File.createTempFile("difi-move", "jks");
                try (BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(tmp))) {
                    tmp.deleteOnExit();
                    FileCopyUtils.copy(serverJksLocation.getInputStream(), output);
                }
                return new FileSystemResource(tmp);
            } catch (IOException ex) {
                logger.error("Can't read keystore", ex);
            }
            return null;
        }

        boolean isPaaVegneAvEnabled() {
            return paaVegneAvEnabled;
        }

        public void setPaaVegneAvEnabled(boolean paaVegneAvEnabled) {
            this.paaVegneAvEnabled = paaVegneAvEnabled;
        }
    }

}

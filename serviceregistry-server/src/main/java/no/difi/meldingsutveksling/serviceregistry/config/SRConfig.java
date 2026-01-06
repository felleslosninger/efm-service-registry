package no.difi.meldingsutveksling.serviceregistry.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.collect.Lists;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.util.Base64;
import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import no.difi.meldingsutveksling.serviceregistry.mvc.ServiceIdentifierConverter;
import no.difi.move.common.cert.KeystoreHelper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.http.MediaType;
import org.springframework.resilience.annotation.EnableResilientMethods;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.security.cert.CertificateEncodingException;
import java.security.interfaces.RSAPublicKey;

@Configuration
@EnableResilientMethods
public class SRConfig implements WebMvcConfigurer {

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(new ServiceIdentifierConverter());
    }

    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        configurer.defaultContentType(MediaType.APPLICATION_JSON);
    }

    @Bean
    public KeystoreHelper keystoreHelper(ServiceregistryProperties srProps) {
        return new KeystoreHelper(srProps.getSign().getKeystore());
    }

    @Bean
    public RSAKey rsaKey(KeystoreHelper keystoreHelper) throws CertificateEncodingException, JOSEException {
        Base64 encodedCert = Base64.encode(keystoreHelper.getX509Certificate().getEncoded());
        return new RSAKey.Builder((RSAPublicKey) keystoreHelper.getKeyPair().getPublic())
                .x509CertChain(Lists.newArrayList(encodedCert))
                .keyUse(KeyUse.SIGNATURE)
                .algorithm(JWSAlgorithm.RS256)
                .keyIDFromThumbprint()
                .build();
    }

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper om = new ObjectMapper();
        om.registerModule(new JavaTimeModule());
        return om;
    }

    @Bean
    public TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }
}

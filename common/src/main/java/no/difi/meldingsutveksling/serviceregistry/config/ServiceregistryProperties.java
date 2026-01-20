package no.difi.meldingsutveksling.serviceregistry.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import no.difi.meldingsutveksling.serviceregistry.domain.ServiceIdentifier;
import no.difi.move.common.config.KeystoreProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.core.io.Resource;

import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Map;

@ConfigurationProperties("difi.move")
@Data
public class ServiceregistryProperties {

    private DigitalPostInnbygger dpi;
    private KontaktOgReservasjonsRegister krr;
    private Brønnøysundregistrene brreg;
    private FregGateway freg;
    private PostVirksomhet dpv;
    private Altinn dpo;
    private Virksert virksert;
    private Auth auth;
    private FeatureToggle feature;
    private ELMA elma;
    private Healthcare healthcare;

    @Valid
    private Sign sign;
    @Valid
    private Fiks fiks;

    @Data
    public static class ELMA {
        private String locatorUrl;
        private String defaultProcessIdentifier;
        private String lookupIcd;
    }

    @Data
    public static class Sign {
        @NotNull
        private KeystoreProperties keystore;
    }

    @Data
    public static class Auth {
        private String sasToken;
        private String maskinportenIssuer;
    }

    @Data
    public static class DigitalPostInnbygger {
        private URL endpointURL;
        private String infoProcess;
        private String vedtakProcess;
        private String printDocumentType;
    }

    @Data
    public static class KontaktOgReservasjonsRegister {
        private URI mpEndpointUri;
        private URI mpDsfEndpointUri;
        private String printUrl;
    }

    @Data
    public static class FeatureToggle {
        private boolean enableDpfDpv = true;
    }

    @Data
    public static class Brønnøysundregistrene {
        private URL endpointURL;
        private boolean enabled;
    }

    @Data
    public static class FregGateway {
        private boolean enabled;
        private String endpointURL;
        private String apiKey;
    }

    @Data
    public static class PostVirksomhet {
        private URL endpointURL;
    }

    @Data
    public static class Altinn {
        private URL endpointURL;
        private String serviceCode;
        private String serviceEditionCode;
        private String resource;
    }

    public record Healthcare(@DefaultValue("false") boolean enabled,
                             String nhnAdapterEndpointUrl,
                             String fastlegeProcess,
                             String nhnProcess) {
    }

    @Data
    public static class Virksert {
        @NotNull
        private URL endpointURL;
        private Map<ServiceIdentifier, String> processes;
        private String icd;
        @NotNull
        private String mode;
    }

    @Data
    public static class Fiks {
        @Valid
        private SvarUt svarut;
        @Valid
        private FiksIo io;
        @Valid
        private Oidc oidc;
    }

    @Data
    public static class Oidc {

        private URL url;
        private String audience;
        private String clientId;

        /**
         * Properties for Certificate
         */
        @NestedConfigurationProperty
        private KeystoreProperties keystore;
    }


    @Data
    public static class FiksIo {
        private boolean enable;
        @NotNull
        private String endpointUrl;
        @NotNull
        private List<String> orgformFilter;
        @NotNull
        private String integrasjonId;
        @NotNull
        private String integrasjonPassord;
    }

    @Data
    public static class SvarUt {
        /**
         * KS fiks SvarUt base url for rest api
         */
        @NotNull
        private URL baseUrl;
        /**
         * The integrasjonid for the integration used for communication with KS fiks SvarUt rest api.
         */
        @NotNull
        private String integrasjonId;
        /**
         * The integrationpassword for the integration used for communication with KS fiks SvarUt rest api.
         */
        @NotNull
        private String integrasjonPassord;
        @NotNull
        private URL serviceRecordUrl;
        @NotNull
        private Resource certificate;
    }
}

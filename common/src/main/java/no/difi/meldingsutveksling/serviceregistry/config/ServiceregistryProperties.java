/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package no.difi.meldingsutveksling.serviceregistry.config;

import lombok.Data;
import no.difi.meldingsutveksling.serviceregistry.domain.ServiceIdentifier;
import no.difi.move.common.config.KeystoreProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Map;

/**
 * @author Nikolai Luthman <nikolai dot luthman at inmeta dot no>
 */
@ConfigurationProperties("difi.move")
@Data
public class ServiceregistryProperties {

    private DigitalPostInnbygger dpi;
    private KontaktOgReservasjonsRegister krr;
    private Brønnøysundregistrene brreg;
    private FregGateway freg;
    private Datahotell datahotell;
    private PostVirksomhet dpv;
    private Altinn dpo;
    private Virksert virksert;
    private Auth auth;
    private FeatureToggle feature;
    private ELMA elma;

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
        private String oidcIssuer;
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
        private URI oidcEndpointUri;
        private URI oidcDsfEndpointUri;
        private String printUrl;
    }

    @Data
    public static class FeatureToggle {
        private boolean enableDpfDpv = true;
    }

    @Data
    public static class Brønnøysundregistrene {
        private URL endpointURL;
    }

    @Data
    public static class FregGateway {
        private String endpointURL;
    }

    @Data
    public static class Datahotell {
        private URL endpointURL;
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
    }

    @Data
    public static class Virksert {
        private URL endpointURL;
        private Map<ServiceIdentifier, String> processes;
        private String icd;
    }

    @Data
    public static class Fiks {
        @Valid
        private SvarUt svarut;
        @Valid
        private FiksIo io;
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
        @NotNull
        private String user;
        @NotNull
        private String password;
        @NotNull
        private URL forsendelsesserviceUrl;
        @NotNull
        private URL serviceRecordUrl;
        @NotNull
        private Resource certificate;
    }
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package no.difi.meldingsutveksling.serviceregistry.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.net.URL;

/**
 *
 * @author Nikolai Luthman <nikolai dot luthman at inmeta dot no>
 */
@ConfigurationProperties("difi.move")
@Data
public class ServiceregistryProperties {

    private DigitalPostInnbygger dpi;
    private KontaktOgReservasjonsRegister krr;
    private Brønnøysundregistrene brreg;
    private Datahotell datahotell;
    private PostVirksomhet dpv;
    private Altinn dpo;
    private Adresseregister ar;
    private Auth auth;
    private FeatureToggle feature;
    private ELMA elma;

    @Valid
    private Sign sign;
    @Valid
    private FIKS fiks = new FIKS();
    @Valid
    private SvarUt svarut;

    @Data
    public static class ELMA {
        private String defaultProcessIdentifier;
        private String processIdentifier;
        private String documentTypeIdentifier;
    }

    @Data
    public static class Sign {
        @NotNull
        private Keystore jks;
    }

    @Data
    public static class Auth {
        private boolean enable;
        private String sasToken;
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
        private URL endpointURL;
        private URL dsfEndpointURL;
        private String printAdress;
        private String printCertificate;
    }

    @Data
    public static class FeatureToggle {
        private boolean paaVegneAvOppslag = false;
    }

    @Data
    public static class Brønnøysundregistrene {
        private URL endpointURL;
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
    public static class Adresseregister {
        private URL endpointURL;
        private String processIdentifier;
        private String schema;
    }

    @Data
    public static class Keystore {
        private String alias;
        private String password;
        private Resource keystore;
    }

    @Data
    public static class FIKS {
        @NotNull
        private URL adresseServiceURL;
        @Valid
        private SvarUt svarut;

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

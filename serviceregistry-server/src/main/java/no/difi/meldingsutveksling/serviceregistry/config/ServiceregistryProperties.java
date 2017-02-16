/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package no.difi.meldingsutveksling.serviceregistry.config;

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
public class ServiceregistryProperties {

    private DigitalPostInnbygger dpi;
    private KontaktOgReservasjonsRegister krr;
    private Brønnøysundregistrene brreg;
    private PostVirksomhet dpv;
    private Adresseregister ar;
    private Auth auth;
    private FeatureToggle feature;
    private ELMA elma;
    @Valid
    private Sign sign;

    public Auth getAuth() {
        return auth;
    }

    public void setAuth(Auth auth) {
        this.auth = auth;
    }

    public DigitalPostInnbygger getDpi() {
        return dpi;
    }

    public void setDpi(DigitalPostInnbygger dpi) {
        this.dpi = dpi;
    }

    public KontaktOgReservasjonsRegister getKrr() {
        return krr;
    }

    public void setKrr(KontaktOgReservasjonsRegister krr) {
        this.krr = krr;
    }

    public Brønnøysundregistrene getBrreg() {
        return brreg;
    }

    public void setBrreg(Brønnøysundregistrene brreg) {
        this.brreg = brreg;
    }

    public PostVirksomhet getDpv() {
        return dpv;
    }

    public void setDpv(PostVirksomhet dpv) {
        this.dpv = dpv;
    }

    public Adresseregister getAr() {
        return ar;
    }

    public void setAr(Adresseregister ar) {
        this.ar = ar;
    }

    public FeatureToggle getFeature() {
        return feature;
    }

    public void setFeature(FeatureToggle feature) {
        this.feature = feature;
    }

    public ELMA getElma() {
        return elma;
    }

    public void setElma(ELMA elma) {
        this.elma = elma;
    }

    public Sign getSign() {
        return sign;
    }

    public void setSign(Sign sign) {
        this.sign = sign;
    }

    public static class ELMA {

        private String processIdentifier;
        private String documentTypeIdentifier;

        public String getProcessIdentifier() {
            return processIdentifier;
        }

        public void setProcessIdentifier(String processIdentifier) {
            this.processIdentifier = processIdentifier;
        }

        public String getDocumentTypeIdentifier() {
            return documentTypeIdentifier;
        }

        public void setDocumentTypeIdentifier(String documentTypeIdentifier) {
            this.documentTypeIdentifier = documentTypeIdentifier;
        }
    }

    public static class Sign {

        @NotNull
        private Keystore jks;

        public Keystore getJks() {
            return jks;
        }

        public void setJks(Keystore jks) {
            this.jks = jks;
        }
    }

    public static class Auth {

        private boolean enable;

        public boolean isEnable() {
            return enable;
        }

        public void setEnable(boolean enable) {
            this.enable = enable;
        }
    }

    public static class DigitalPostInnbygger {

        private URL endpointURL;

        public URL getEndpointURL() {
            return endpointURL;
        }

        public void setEndpointURL(URL endpointURL) {
            this.endpointURL = endpointURL;
        }
    }

    public static class KontaktOgReservasjonsRegister {

        private URL endpointURL;

        private Keystore client;

        private Keystore server;

        public URL getEndpointURL() {
            return endpointURL;
        }

        public void setEndpointURL(URL endpointURL) {
            this.endpointURL = endpointURL;
        }

        public Keystore getClient() {
            return client;
        }

        public void setClient(Keystore client) {
            this.client = client;
        }

        public Keystore getServer() {
            return server;
        }

        public void setServer(Keystore server) {
            this.server = server;
        }
    }

    public static class FeatureToggle {
        private boolean paaVegneAvOppslag = false;

        public boolean isPaaVegneAvOppslag() {
            return paaVegneAvOppslag;
        }

        public void setPaaVegneAvOppslag(boolean paaVegneAvOppslag) {
            this.paaVegneAvOppslag = paaVegneAvOppslag;
        }
    }

    public static class Brønnøysundregistrene {

        private URL endpointURL;

        public URL getEndpointURL() {
            return endpointURL;
        }

        public void setEndpointURL(URL endpointURL) {
            this.endpointURL = endpointURL;
        }
    }

    public static class PostVirksomhet {

        private URL endpointURL;

        public URL getEndpointURL() {
            return endpointURL;
        }

        public void setEndpointURL(URL endpointURL) {
            this.endpointURL = endpointURL;
        }
    }

    public static class Adresseregister {

        private URL endpointURL;

        public URL getEndpointURL() {
            return endpointURL;
        }

        public void setEndpointURL(URL endpointURL) {
            this.endpointURL = endpointURL;
        }
    }

    public static class Keystore {

        private String alias;
        private String password;
        private Resource keystore;

        public String getAlias() {
            return alias;
        }

        public void setAlias(String alias) {
            this.alias = alias;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public Resource getKeystore() {
            return keystore;
        }

        public void setKeystore(Resource keystore) {
            this.keystore = keystore;
        }

    }
}

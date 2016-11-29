package no.difi.meldingsutveksling.ptp;

import no.difi.ptp.sikkerdigitalpost.HentPrintSertifikatRespons;

public class PrintProviderDetails {

    private final String postkasseleverandoerAdresse;
    private final String pemCertificate;

    public PrintProviderDetails(String postkasseleverandoerAdresse, String pemCertificate) {
        this.postkasseleverandoerAdresse = postkasseleverandoerAdresse;
        this.pemCertificate = pemCertificate;
    }

    public static PrintProviderDetails from(HentPrintSertifikatRespons response) {
        return new PrintProviderDetails(response.getPostkasseleverandoerAdresse(), CertificateUtil.pemCertificateFrom(response.getX509Sertifikat()));
    }

    public String getPostkasseleverandoerAdresse() {
        return postkasseleverandoerAdresse;
    }

    public String getPemCertificate() {
        return pemCertificate;
    }
}

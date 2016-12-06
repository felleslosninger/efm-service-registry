package no.difi.meldingsutveksling.ptp;

import no.difi.meldingsutveksling.ptp.OppslagstjenesteClient.Configuration;
import org.springframework.core.io.FileSystemResource;

import static no.difi.meldingsutveksling.serviceregistry.krr.LookupParameters.lookup;

public class OppslagstjenesteClientMain {

    public static void main(String[] args) {
        final Configuration configuration = new Configuration("https://kontaktinfo-ws-ver2.difi.no/kontaktinfo-external/ws-v5", "changeit", "client_alias", "ver2", new FileSystemResource("kontaktinfo-client-test.jks"), new FileSystemResource("kontaktinfo-server-test.jks"));
        OppslagstjenesteClient client = new OppslagstjenesteClient(configuration);
        final KontaktInfo kontaktInfo = client.hentKontaktInformasjon(lookup("06068700602"));
        System.out.println(kontaktInfo);
    }
}

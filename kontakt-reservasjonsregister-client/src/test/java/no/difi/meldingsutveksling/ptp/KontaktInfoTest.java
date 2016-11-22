package no.difi.meldingsutveksling.ptp;

import no.difi.ptp.sikkerdigitalpost.HentPersonerRespons;
import no.difi.ptp.sikkerdigitalpost.Person;
import org.junit.Test;

public class KontaktInfoTest {
    @Test
    public void testCreateKontaktInfoFromHentPersonerResponse() throws Exception {
        HentPersonerRespons hentPersonerRespons = new HentPersonerRespons();
        hentPersonerRespons.getPerson().add(new Person());
        KontaktInfo.from(hentPersonerRespons);

    }

}
package no.difi.meldingsutveksling.ptp;

import no.difi.ptp.sikkerdigitalpost.Person;
import org.junit.Test;

public class PersonKontaktInfoMapperTest {
    @Test
    public void testMappingPersonToKontaktInfo() throws Exception {
        Person person = new Person();
        PersonKontaktInfoMapper.map(person);

    }

}
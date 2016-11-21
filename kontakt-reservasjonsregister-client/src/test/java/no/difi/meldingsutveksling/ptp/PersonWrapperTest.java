package no.difi.meldingsutveksling.ptp;

import no.difi.ptp.sikkerdigitalpost.Person;
import no.difi.ptp.sikkerdigitalpost.SikkerDigitalPostAdresse;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PersonWrapperTest {

    @Test
    public void hasMailBox() {
        Person person = new Person();
        SikkerDigitalPostAdresse sdp = new SikkerDigitalPostAdresse();
        sdp.setPostkasseadresse("123456789");
        sdp.setPostkasseleverandoerAdresse("http://localhost");
        person.setX509Sertifikat(new byte[1]);
        person.setSikkerDigitalPostAdresse(sdp);

        PersonWrapper personWrapper = new PersonWrapper(person);

        assertTrue(personWrapper.hasMailbox());
    }

    @Test
    public void doesNotHaveMailBox() {
        Person person = new Person();

        PersonWrapper personWrapper = new PersonWrapper(person);

        assertFalse(personWrapper.hasMailbox());
    }

    @Test
    public void getValuesOnEmptyPerson() {
        Person person = new Person();

        PersonWrapper personWrapper = new PersonWrapper(person);

        personWrapper.getX509Sertifikat();
    }
}
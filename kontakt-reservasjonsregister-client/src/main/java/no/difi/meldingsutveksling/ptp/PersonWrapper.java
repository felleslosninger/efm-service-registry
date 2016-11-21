package no.difi.meldingsutveksling.ptp;

import no.difi.ptp.sikkerdigitalpost.Person;

public class PersonWrapper {
    private final Person person;

    public PersonWrapper(Person person) {
        this.person = person;
    }


    public byte[] getX509Sertifikat() {
        return person.getX509Sertifikat();
    }

    public boolean hasMailbox() {
        return hasCertificate() && hasMailboxUrl() && hasMailboxOrgnumber();
    }

    public boolean hasCertificate() {
        return person.getX509Sertifikat() != null;
    }

    private boolean hasMailboxOrgnumber() {
        return hasSikkerDigitalPostAdresse() && person.getSikkerDigitalPostAdresse().getPostkasseadresse() != null;
    }

    private boolean hasMailboxUrl() {
        return hasSikkerDigitalPostAdresse() && person.getSikkerDigitalPostAdresse().getPostkasseleverandoerAdresse() != null;
    }

    private boolean hasSikkerDigitalPostAdresse() {
        return person.getSikkerDigitalPostAdresse() != null;
    }
}

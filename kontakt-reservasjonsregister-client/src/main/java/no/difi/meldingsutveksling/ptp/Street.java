package no.difi.meldingsutveksling.ptp;

public class Street {
    private final String street1;
    private final String street2;
    private final String street3;
    private final String street4;

    public Street(String street1, String street2, String street3, String street4) {
        this.street1 = street1;
        this.street2 = street2;
        this.street3 = street3;
        this.street4 = street4;
    }

    String getStreet1() {
        return street1;
    }

    String getStreet2() {
        return street2;
    }

    String getStreet3() {
        return street3;
    }

    String getStreet4() {
        return street4;
    }
}

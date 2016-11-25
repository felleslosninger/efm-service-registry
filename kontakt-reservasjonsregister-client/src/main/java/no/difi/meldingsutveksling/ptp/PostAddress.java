package no.difi.meldingsutveksling.ptp;

import com.google.common.base.MoreObjects;

public class PostAddress {
    private final String name;
    private String street1;
    private String street2;
    private String street3;
    private String street4;
    private final String postalCode;
    private final String postalArea;
    private final String country;

    public PostAddress(String name, String street1, String street2, String street3, String street4, String postalCode, String postalArea, String country) {
        this.name = name;
        this.street1 = street1;
        this.street2 = street2;
        this.street3 = street3;
        this.street4 = street4;
        this.postalCode = postalCode;
        this.postalArea = postalArea;
        this.country = country;
    }

    public String getStreet1() {
        return street1;
    }

    public String getStreet2() {
        return street2;
    }

    public String getStreet3() {
        return street3;
    }

    public String getStreet4() {
        return street4;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public String getPostalArea() {
        return postalArea;
    }

    public String getCountry() {
        return country;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("name", name)
                .add("street1", street1)
                .add("street2", street2)
                .add("street3", street3)
                .add("street4", street4)
                .add("postalCode", postalCode)
                .add("postalArea", postalArea)
                .add("country", country)
                .toString();
    }
}

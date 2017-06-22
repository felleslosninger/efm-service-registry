package no.difi.meldingsutveksling.serviceregistry.krr;

import lombok.Data;

@Data
public class PostAddress {
    private final String name;
    private final String street;
    private final String postalCode;
    private final String postalArea;
    private final String country;

    public PostAddress(String name, String street, String postalCode, String postalArea, String country) {
        this.name = name;
        this.street = street;
        this.postalCode = postalCode;
        this.postalArea = postalArea;
        this.country = country;
    }

}

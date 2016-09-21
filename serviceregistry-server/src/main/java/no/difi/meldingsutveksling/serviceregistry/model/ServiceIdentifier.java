package no.difi.meldingsutveksling.serviceregistry.model;

public enum ServiceIdentifier {

    EDU("EDU"), POST_VIRKSOMHET("POST_VIRKSOMHET"), SIKKER_DIGITAL_POST("SDP");
    private String name;

    ServiceIdentifier(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }


    @Override
    public String toString() {
        return name;
    }
}

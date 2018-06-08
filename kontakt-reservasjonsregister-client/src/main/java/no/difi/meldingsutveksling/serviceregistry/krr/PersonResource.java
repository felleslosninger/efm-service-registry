package no.difi.meldingsutveksling.serviceregistry.krr;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import static com.google.common.base.Strings.isNullOrEmpty;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PersonResource {

    @JsonProperty(value = "personidentifikator", required = true)
    private String personIdentifier;

    @JsonProperty(value = "reservasjon", required = true)
    private String reserved;

    @JsonProperty(value = "status", required = true)
    private String status;

    @JsonProperty(value = "varslingsstatus")
    private String alertStatus;

    @JsonProperty(value = "kontaktinformasjon")
    private ContactInfoResource contactInfo;

    @JsonProperty(value = "digital_post")
    private DigitalPostResource digitalPost;

    @JsonProperty(value = "sertifikat")
    private String certificate;

    private String printPostkasseLeverandorAdr;

    public boolean hasMailbox() {
        return !isNullOrEmpty(certificate) && digitalPost != null;
    }

    public boolean canReceiveDigitalPost() {
        return hasMailbox() && Reservasjon.NEI.toString().equals(getReserved()) && Status.AKTIV.toString().equals(getStatus());
    }

    public boolean isReserved() {
        return Reservasjon.JA.toString().equals(getReserved());
    }

    public boolean isActive() {
        return Status.AKTIV.toString().equals(getStatus());
    }

    public boolean isNotifiable() {
        return Varslingsstatus.KAN_VARSLES.toString().equals(getAlertStatus());
    }

    public enum Reservasjon {
        JA,
        NEI
    }

    public enum Status {
        AKTIV,
        SLETTET,
        IKKE_REGISTRERT
    }

    public enum Varslingsstatus {
        KAN_IKKE_VARSLES,
        KAN_VARSLES
    }

}

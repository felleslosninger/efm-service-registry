package no.difi.meldingsutveksling.serviceregistry.freg.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.ArrayList;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FregGatewayEntity {

    @Data
    @Builder
    public static class Address {

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Response {
            PostAdresse postadresse = new PostAdresse();
            String personIdentifikator;
            Navn navn;
        }

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Navn {

            String fornavn;
            String etternavn;
            String mellomnavn;
            String forkortetNavn;
        }

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class PostAdresse {

            ArrayList<String> adresselinje = new ArrayList<String>();
            String postnummer;
            String poststed;
            String landkode;
            String adressegradering;
        }
    }
}

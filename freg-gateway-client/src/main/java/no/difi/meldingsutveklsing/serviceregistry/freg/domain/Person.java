package no.difi.meldingsutveklsing.serviceregistry.freg.domain;

import java.util.Map;

public class Person {

    private Map<String, Object> navn;
    private Map<String, Object> postadresse;
    private String personIdentifikator;


}


/**
 * {
 *   "navn": {
 *     "fornavn": "TILGIVENDE",
 *     "etternavn": "BALLSKO",
 *     "mellomnavn": null,
 *     "forkortetNavn": null
 *   },
 *   "postadresse": {
 *     "adresselinje": ["Dagaliveien 12B"],
 *     "postnummer": "0776",
 *     "poststed": "OSLO",
 *     "landkode": null,
 *     "adressegradering": "ugradert"
 *   },
 *   "personIdentifikator": "28825897617"
 * }
 *
 * @Data
 * @JsonInclude(JsonInclude.Include.NON_NULL)
 * public class DsfMpResource {
 *
 *     @JsonProperty(value = "personIdentifikator", required = true)
 *     private String personIdentifier;
 *
 *     @JsonProperty(value = "navn", required = true)
 *     private DsfMpNavn navn;
 *
 *     @JsonProperty(value = "postadresse", required = true)
 *     private DsfMpPostadresse postadresse;
 * }
 *
 *
 *
 */
package no.difi.meldingsutveksling.serviceregistry.service.dph;

import org.apache.commons.lang3.tuple.Triple;
import org.springframework.stereotype.Component;

@Component
public class NhnService {


    public ARDetails getARDetails(String fnr) {
        return new ARDetails("8143143","8143145","dummyCertificate","meldingstjener-api@testedi.nhn.no");
    }

}



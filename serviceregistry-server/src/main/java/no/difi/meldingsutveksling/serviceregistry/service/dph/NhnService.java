package no.difi.meldingsutveksling.serviceregistry.service.dph;

import no.difi.meldingsutveksling.serviceregistry.record.LookupParameters;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.RequestEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class NhnService {

    final private String uri;

    public NhnService(@Value("difi.move.nhn-adapter.endpointURL") String uri) {
        this.uri = uri;
    }


    public ARDetails getARDetails(LookupParameters param) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + param.getToken().getTokenValue());
        headers.set("Accept", "application/json");

        RequestEntity<Void> requestEntity =  RequestEntity.get(uri,param.getIdentifier()).headers(headers).build();

        RestTemplate rt = new RestTemplate();
        return rt.exchange(requestEntity,ARDetails.class).getBody();

       // return new ARDetails("8143143","8143145","dummyCertificate","meldingstjener-api@testedi.nhn.no");
    }




}



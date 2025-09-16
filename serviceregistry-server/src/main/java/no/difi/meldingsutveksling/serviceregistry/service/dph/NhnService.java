package no.difi.meldingsutveksling.serviceregistry.service.dph;

import no.difi.meldingsutveksling.serviceregistry.record.LookupParameters;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.RequestEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.URL;


public class NhnService {

    final private String uri;

    public NhnService(String nhnUri) {
        this.uri = nhnUri;
    }


    public ARDetails getARDetails(LookupParameters param) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + param.getToken().getTokenValue());
        headers.set("Accept", "application/json");


        RequestEntity<Void> requestEntity =  RequestEntity.get(uri,param.getIdentifier()).headers(headers).build();

        RestTemplate rt = new RestTemplate();
        return rt.exchange(requestEntity,ARDetails.class).getBody();
    }




}



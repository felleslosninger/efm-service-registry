package no.difi.meldingsutveksling.serviceregistry.freg.client;

import no.difi.meldingsutveksling.serviceregistry.config.ServiceregistryProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import no.difi.meldingsutveksling.serviceregistry.freg.domain.FregGatewayEntity;

import java.net.URI;
import java.net.URL;
import java.util.Optional;


@Component
@RequiredArgsConstructor
public class FregGatewayClient {
    private String fregGatewayUri;
    private ServiceregistryProperties properties;

    @Autowired
    @Qualifier("fregGatewayRestTemplate")
    private RestTemplate restTemplate;

    @PostConstruct
    public void init(){
        this.fregGatewayUri = properties.getFregGateway().getEndpointURL();
    }

    public Optional<FregGatewayEntity.Address.Response> getPersonAdress(String pid){
        ResponseEntity<FregGatewayEntity.Address.Response> response = restTemplate.getForEntity(
                fregGatewayUri + "person/personadresse/{pid}",
                FregGatewayEntity.Address.Response.class, pid);
        return Optional.of(response.getBody());
    }

}

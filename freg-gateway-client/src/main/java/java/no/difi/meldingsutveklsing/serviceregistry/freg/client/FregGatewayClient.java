package java.no.difi.meldingsutveklsing.serviceregistry.freg.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.no.difi.meldingsutveklsing.serviceregistry.freg.domain.FregGatewayEntity;
import java.util.Optional;


@Component
@RequiredArgsConstructor
public class FregGatewayClient {
    private String fregGatewayUrl;

    @Autowired
    @Qualifier("fregGatewayRestTemplate")
    private RestTemplate restTemplate;

    @PostConstruct
    public void init(){
        this.fregGatewayUrl = "localhost:8099/person/personadresse/{pid}";
    }

    public Optional<FregGatewayEntity.Address.Response> getPersonAdress(String pid){
        ResponseEntity<FregGatewayEntity.Address.Response> response = restTemplate.getForEntity(
                fregGatewayUrl, FregGatewayEntity.Address.Response.class, pid);
        return Optional.of(response.getBody());
    }

}

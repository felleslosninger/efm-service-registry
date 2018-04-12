package no.difi.meldingsutveksling.serviceregistry.svarut;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SvarUtService {

    private SvarUtClient svarUtClient;

    @Autowired
    SvarUtService(SvarUtClient svarUtClient) {
        this.svarUtClient = svarUtClient;
    }

    public boolean hasSvarUtAdressering(String orgnr) {
        RetrieveMottakerSystemForOrgnr request = RetrieveMottakerSystemForOrgnr.builder().withOrganisasjonsnr(orgnr).build();
        RetrieveMottakerSystemForOrgnrResponse response = svarUtClient.retrieveMottakerSystemForOrgnr(request);
        return !response.getReturn().isEmpty();
    }
}

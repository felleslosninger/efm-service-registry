package no.difi.meldingsutveksling.serviceregistry.servicerecord;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import static no.difi.meldingsutveksling.serviceregistry.model.ServiceIdentifier.POST_VIRKSOMHET;


public class PostVirksomhetServiceRecord extends ServiceRecord {

    public static final String CONFIG_KEY_ENDPOINT = "postvirksomhet.endPointURL";

    @Autowired
    public PostVirksomhetServiceRecord(Environment environment, String orgnr) {
        super(environment, null, POST_VIRKSOMHET, orgnr);
    }

    @Override
    public String getEndPointURL() {
        return environment.getProperty(CONFIG_KEY_ENDPOINT);
    }
}

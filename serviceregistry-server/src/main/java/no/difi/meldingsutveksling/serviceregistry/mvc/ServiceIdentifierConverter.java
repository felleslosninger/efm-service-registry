package no.difi.meldingsutveksling.serviceregistry.mvc;

import no.difi.meldingsutveksling.serviceregistry.domain.ServiceIdentifier;
import org.springframework.core.convert.converter.Converter;

public class ServiceIdentifierConverter implements Converter<String, ServiceIdentifier> {

    @Override
    public ServiceIdentifier convert(String source) {
        try {
            return ServiceIdentifier.valueOf(source.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new UnknownServiceIdentifierException(source);
        }
    }

}

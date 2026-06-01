package no.difi.meldingsutveksling.serviceregistry.converter;

import no.difi.meldingsutveksling.domain.PartnerIdentifier;
import org.jspecify.annotations.NonNull;
import org.springframework.core.convert.converter.Converter;

public class StringToPartnerIdentifierConverter implements Converter<String, PartnerIdentifier> {

    @Override
    public PartnerIdentifier convert(@NonNull String source) {
        return PartnerIdentifier.parse(source);
    }
}

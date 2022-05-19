package no.difi.meldingsutveksling.serviceregistry.mvc;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.domain.ICD;
import no.difi.meldingsutveksling.domain.Iso6523;
import no.difi.meldingsutveksling.domain.PartnerIdentifier;
import no.difi.meldingsutveksling.serviceregistry.SRRequestScope;
import no.difi.meldingsutveksling.serviceregistry.businesslogic.ServiceRecordPredicates;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PartnerIdentifierConverter implements Converter<String, PartnerIdentifier> {

    private final SRRequestScope requestScope;

    @Override
    public PartnerIdentifier convert(@NotNull String source) {
        if (ServiceRecordPredicates.isOrgnr().test(source)) {
            requestScope.setUsePlainFormat(true);
            return Iso6523.of(ICD.NO_ORG, source);
        }

        try {
            return PartnerIdentifier.parse(source);
        } catch (IllegalArgumentException e) {
            throw new UnknownIdentifierException("Failed to convert identifier "+source, e);
        }

    }

}

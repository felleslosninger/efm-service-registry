package no.difi.meldingsutveksling.serviceregistry.service.brreg;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.domain.Iso6523;
import no.difi.meldingsutveksling.serviceregistry.SRRequestScope;
import no.difi.meldingsutveksling.serviceregistry.client.brreg.BrregClient;
import no.difi.meldingsutveksling.serviceregistry.domain.BrregEnhet;
import no.difi.meldingsutveksling.serviceregistry.domain.EntityInfo;
import no.difi.meldingsutveksling.serviceregistry.domain.OrganizationInfo;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class BrregService {

    private final BrregClient brregClient;
    private final DatahotellClient datahotellClient;
    private final SRRequestScope requestScope;

    public Optional<EntityInfo> getOrganizationInfo(Iso6523 identifier) throws BrregNotFoundException {
        Optional<BrregEnhet> entity = brregClient.getBrregEnhetByOrgnr(identifier.getPrimaryIdentifier());
        if (entity.isEmpty()) {
            entity = brregClient.getBrregUnderenhetByOrgnr(identifier.getPrimaryIdentifier());
        }
        if (entity.isEmpty()) {
            return datahotellClient.getOrganizationInfo(identifier.getPrimaryIdentifier());
        }

        return entity.map(e -> OrganizationInfo.of(e, requestScope.isUsePlainFormat()));
    }

}

package no.difi.meldingsutveksling.serviceregistry.service.brreg;

import no.difi.meldingsutveksling.serviceregistry.config.ServiceregistryProperties;
import no.difi.meldingsutveksling.serviceregistry.domain.DatahotellEntry;
import no.difi.meldingsutveksling.serviceregistry.domain.EntityInfo;
import no.difi.meldingsutveksling.serviceregistry.domain.OrganizationType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DatahotellClientTest {

    private static final String ORGNR = "987654321";

    @Spy
    private final ServiceregistryProperties props = new ServiceregistryProperties();

    @Spy
    @InjectMocks
    private DatahotellClient target;

    private final DatahotellEntry datahotellEntry = new DatahotellEntry()
            .setOrgnr(ORGNR);

    @Test
    void getOrganizationInfoHovedenhet() throws BrregNotFoundException {
        willReturn(Optional.of(datahotellEntry)).given(target).getHovedenhet(any());
        Optional<EntityInfo> organizationInfo = target.getOrganizationInfo("987 654 321");
        assertThat(organizationInfo).isPresent();
        EntityInfo entityInfo = organizationInfo.get();
        assertThat(entityInfo.getIdentifier()).isEqualTo(ORGNR);
        assertThat(entityInfo.getEntityType()).isInstanceOf(OrganizationType.class);
        verify(target).getHovedenhet(ORGNR);
    }

    @Test
    void getOrganizationInfoUnderenhet() throws BrregNotFoundException {
        willReturn(Optional.empty()).given(target).getHovedenhet(any());
        willReturn(Optional.of(datahotellEntry)).given(target).getUnderenhet(any());
        Optional<EntityInfo> organizationInfo = target.getOrganizationInfo("987 654 321");
        assertThat(organizationInfo).isPresent();
        EntityInfo entityInfo = organizationInfo.get();
        assertThat(entityInfo.getIdentifier()).isEqualTo(ORGNR);
        assertThat(entityInfo.getEntityType()).isInstanceOf(OrganizationType.class);
        verify(target).getHovedenhet(ORGNR);
        verify(target).getUnderenhet(ORGNR);
    }

    @Test
    void trim() {
        assertThat(target.trim(ORGNR)).isEqualTo(ORGNR);
        assertThat(target.trim("987 654 321")).isEqualTo(ORGNR);
        assertThat(target.trim(" 987654321 ")).isEqualTo(ORGNR);
    }
}
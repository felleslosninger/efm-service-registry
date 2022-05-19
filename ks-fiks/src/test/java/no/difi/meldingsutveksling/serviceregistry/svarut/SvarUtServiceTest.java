package no.difi.meldingsutveksling.serviceregistry.svarut;

import com.google.common.collect.Lists;
import no.difi.meldingsutveksling.domain.ICD;
import no.difi.meldingsutveksling.domain.Iso6523;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SvarUtServiceTest {

    @InjectMocks
    private SvarUtService target;

    @Mock
    private SvarUtClient svarUtClientMock;

    private final Iso6523 FIKS_ORGNR = Iso6523.of(ICD.NO_ORG, "123456789");

    @Test
    public void hasSvarUtAddressering_OrganizationNotFound_FiksReceiverResponseShouldBeEmpty() throws SvarUtClientException {
        RetrieveMottakerSystemForOrgnrResponse emptyResponse = RetrieveMottakerSystemForOrgnrResponse.builder()
                .withReturn(Lists.newArrayList()).build();
        when(svarUtClientMock.retrieveMottakerSystemForOrgnr(any(RetrieveMottakerSystemForOrgnr.class)))
                .thenReturn(emptyResponse);

        Optional<Integer> result = target.hasSvarUtAdressering(FIKS_ORGNR, null);

        assertFalse(result.isPresent());
    }

    @Test
    public void hasSvarUtAddressering_OrganizationWithEmptyForsendelseTypeFound_FiksReceiverResponseShouldBePresent() throws SvarUtClientException {
        MottakerForsendelseTyper responseContent = MottakerForsendelseTyper.builder()
                .withForsendelseType(null).build();
        RetrieveMottakerSystemForOrgnrResponse response = RetrieveMottakerSystemForOrgnrResponse.builder()
                .withReturn(Lists.newArrayList(responseContent)).build();
        when(svarUtClientMock.retrieveMottakerSystemForOrgnr(any(RetrieveMottakerSystemForOrgnr.class)))
                .thenReturn(response);

        Optional<Integer> result = target.hasSvarUtAdressering(FIKS_ORGNR, null);

        assertTrue(result.isPresent());
    }

    @Test
    public void hasSvarUtAddressering_OrganizationWithForsendelseTypeFound_FiksReceiverResponseShouldBeEmpty() throws SvarUtClientException {
        MottakerForsendelseTyper responseContent = MottakerForsendelseTyper.builder()
                .withForsendelseType("type").build();
        RetrieveMottakerSystemForOrgnrResponse response = RetrieveMottakerSystemForOrgnrResponse.builder()
                .withReturn(Lists.newArrayList(responseContent)).build();
        when(svarUtClientMock.retrieveMottakerSystemForOrgnr(any(RetrieveMottakerSystemForOrgnr.class)))
                .thenReturn(response);

        Optional<Integer> result = target.hasSvarUtAdressering(FIKS_ORGNR, null);

        assertFalse(result.isPresent());
    }

    @Test
    public void hasSvarUtAddressering_ReceiverNotFound_FiksReceiverResponseShouldBeEmpty() throws SvarUtClientException {
        RetrieveMottakerSystemForOrgnrResponse emptyResponse = RetrieveMottakerSystemForOrgnrResponse.builder()
                .withReturn(Lists.newArrayList()).build();
        when(svarUtClientMock.retrieveMottakerSystemForOrgnr(any(RetrieveMottakerSystemForOrgnr.class)))
                .thenReturn(emptyResponse);

        Optional<Integer> result = target.hasSvarUtAdressering(FIKS_ORGNR, 0);

        assertFalse(result.isPresent());
    }

    @Test
    public void hasSvarUtAddressering_ReceiverFoundWithoutTargetServiceLevel_FiksReceiverResponseShouldBeEmpty() throws SvarUtClientException {
        final int targetLevel = 3;
        MottakerForsendelseTyper responseContent = MottakerForsendelseTyper.builder()
                .withForsendelseType(null)
                .withNiva(targetLevel).build();
        RetrieveMottakerSystemForOrgnrResponse response = RetrieveMottakerSystemForOrgnrResponse.builder()
                .withReturn(Lists.newArrayList(responseContent)).build();
        when(svarUtClientMock.retrieveMottakerSystemForOrgnr(any(RetrieveMottakerSystemForOrgnr.class)))
                .thenReturn(response);

        Optional<Integer> result = target.hasSvarUtAdressering(FIKS_ORGNR, 0);

        assertFalse(result.isPresent());
    }

    @Test
    public void hasSvarUtAddressering_ReceiverFoundWithTargetServiceLevelAndForsendelseType_FiksReceiverResponseShouldBeEmpty() throws SvarUtClientException {
        final int targetLevel = 3;
        MottakerForsendelseTyper responseContent = MottakerForsendelseTyper.builder()
                .withForsendelseType("type")
                .withNiva(targetLevel).build();
        RetrieveMottakerSystemForOrgnrResponse response = RetrieveMottakerSystemForOrgnrResponse.builder()
                .withReturn(Lists.newArrayList(responseContent)).build();
        when(svarUtClientMock.retrieveMottakerSystemForOrgnr(any(RetrieveMottakerSystemForOrgnr.class)))
                .thenReturn(response);

        Optional<Integer> result = target.hasSvarUtAdressering(FIKS_ORGNR, targetLevel);

        assertFalse(result.isPresent());
    }

    @Test
    public void hasSvarUtAddressering_ReceiverFoundWithTargetServiceLevel_FiksReceiverResponseShouldBePresent() throws SvarUtClientException {
        final int targetLevel = 3;
        MottakerForsendelseTyper responseContent = MottakerForsendelseTyper.builder()
                .withForsendelseType(null)
                .withNiva(targetLevel).build();
        RetrieveMottakerSystemForOrgnrResponse response = RetrieveMottakerSystemForOrgnrResponse.builder()
                .withReturn(Lists.newArrayList(responseContent)).build();
        when(svarUtClientMock.retrieveMottakerSystemForOrgnr(any(RetrieveMottakerSystemForOrgnr.class)))
                .thenReturn(response);

        Optional<Integer> result = target.hasSvarUtAdressering(FIKS_ORGNR, targetLevel);

        assertTrue(result.isPresent());
    }

}

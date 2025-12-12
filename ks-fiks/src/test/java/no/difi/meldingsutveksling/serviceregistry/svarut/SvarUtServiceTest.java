package no.difi.meldingsutveksling.serviceregistry.svarut;

import no.difi.meldingsutveksling.serviceregistry.svarut.mottakersystem.Mottakersystem;
import no.difi.meldingsutveksling.serviceregistry.svarut.mottakersystem.Mottakersystemer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SvarUtServiceTest {

    @InjectMocks
    private SvarUtService target;

    @Mock
    private SvarUtClient svarUtClientMock;

    @Test
    public void hasSvarUtAddressering_OrganizationNotFound_FiksReceiverResponseShouldBeEmpty() throws SvarUtClientException {
        Mottakersystemer mottakersystemer = new Mottakersystemer();

        when(svarUtClientMock.retrieveMottakerSystemForOrgnr("123456789"))
                .thenReturn(mottakersystemer);

        Optional<Integer> result = target.hasSvarUtAdressering("123456789", null);

        assertFalse(result.isPresent());
    }

    @Test
    public void hasSvarUtAddressering_OrganizationWithEmptyForsendelseTypeFound_FiksReceiverResponseShouldBePresent() throws SvarUtClientException {
        Mottakersystem system = new Mottakersystem();
        system.setNiva(0);
        Mottakersystemer mottakersystemer = new Mottakersystemer();
        mottakersystemer.addMottakersystemerItem(system);

        when(svarUtClientMock.retrieveMottakerSystemForOrgnr("123456789"))
                .thenReturn(mottakersystemer);

        Optional<Integer> result = target.hasSvarUtAdressering("123456789", null);

        assertTrue(result.isPresent());
    }

    @Test
    public void hasSvarUtAddressering_OrganizationWithForsendelseTypeFound_FiksReceiverResponseShouldBeEmpty() throws SvarUtClientException {
        Mottakersystem system = new Mottakersystem();
        system.setForsendelseType("type");
        Mottakersystemer mottakersystemer = new Mottakersystemer();
        mottakersystemer.addMottakersystemerItem(system);

        when(svarUtClientMock.retrieveMottakerSystemForOrgnr("123456789"))
                .thenReturn(mottakersystemer);

        Optional<Integer> result = target.hasSvarUtAdressering("123456789", null);

        assertFalse(result.isPresent());
    }

    @Test
    public void hasSvarUtAddressering_ReceiverNotFound_FiksReceiverResponseShouldBeEmpty() throws SvarUtClientException {

        Mottakersystemer mottakersystemer = new Mottakersystemer();

        when(svarUtClientMock.retrieveMottakerSystemForOrgnr("123456789"))
                .thenReturn(mottakersystemer);

        Optional<Integer> result = target.hasSvarUtAdressering("123456789", 0);

        assertFalse(result.isPresent());
    }

    @Test
    public void hasSvarUtAddressering_ReceiverFoundWithoutTargetServiceLevel_FiksReceiverResponseShouldBeEmpty() throws SvarUtClientException {
        final int targetLevel = 3;
        Mottakersystem system = new Mottakersystem();
        system.setNiva(targetLevel);
        Mottakersystemer mottakersystemer = new Mottakersystemer();
        mottakersystemer.addMottakersystemerItem(system);

        when(svarUtClientMock.retrieveMottakerSystemForOrgnr("123456789"))
                .thenReturn(mottakersystemer);

        Optional<Integer> result = target.hasSvarUtAdressering("123456789", 0);

        assertFalse(result.isPresent());
    }

    @Test
    public void hasSvarUtAddressering_ReceiverFoundWithTargetServiceLevelAndForsendelseType_FiksReceiverResponseShouldBeEmpty() throws SvarUtClientException {
        final int targetLevel = 3;
        Mottakersystem system = new Mottakersystem();
        system.setForsendelseType("type");
        system.setNiva(targetLevel);
        Mottakersystemer mottakersystemer = new Mottakersystemer();
        mottakersystemer.addMottakersystemerItem(system);

        when(svarUtClientMock.retrieveMottakerSystemForOrgnr("123456789"))
                .thenReturn(mottakersystemer);

        Optional<Integer> result = target.hasSvarUtAdressering("123456789", targetLevel);

        assertFalse(result.isPresent());
    }

    @Test
    public void hasSvarUtAddressering_ReceiverFoundWithTargetServiceLevel_FiksReceiverResponseShouldBePresent() throws SvarUtClientException {
        final int targetLevel = 3;
        Mottakersystem system = new Mottakersystem();
        system.setNiva(targetLevel);
        Mottakersystemer mottakersystemer = new Mottakersystemer();
        mottakersystemer.addMottakersystemerItem(system);

        when(svarUtClientMock.retrieveMottakerSystemForOrgnr("123456789"))
                .thenReturn(mottakersystemer);

        Optional<Integer> result = target.hasSvarUtAdressering("123456789", targetLevel);

        assertTrue(result.isPresent());
    }

}

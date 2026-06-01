package no.difi.meldingsutveksling.serviceregistry.service;

import no.difi.meldingsutveksling.domain.FiksIoIdentifier;
import no.difi.meldingsutveksling.domain.Iso6523;
import no.difi.meldingsutveksling.domain.NhnIdentifier;
import no.difi.meldingsutveksling.domain.PersonIdentifier;
import no.difi.meldingsutveksling.serviceregistry.SRRequestScope;
import no.difi.meldingsutveksling.serviceregistry.domain.CitizenInfo;
import no.difi.meldingsutveksling.serviceregistry.domain.EntityInfo;
import no.difi.meldingsutveksling.serviceregistry.domain.FiksIoInfo;
import no.difi.meldingsutveksling.serviceregistry.domain.HelseEnhetInfo;
import no.difi.meldingsutveksling.serviceregistry.domain.OrganizationInfo;
import no.difi.meldingsutveksling.serviceregistry.domain.OrganizationType;
import no.difi.meldingsutveksling.serviceregistry.exceptions.EntityNotFoundException;
import no.difi.meldingsutveksling.serviceregistry.fiks.io.FiksIoService;
import no.difi.meldingsutveksling.serviceregistry.service.brreg.BrregNotFoundException;
import no.difi.meldingsutveksling.serviceregistry.service.brreg.BrregService;
import no.difi.meldingsutveksling.serviceregistry.service.healthcare.HealthAddressRegistryDetails;
import no.difi.meldingsutveksling.serviceregistry.service.healthcare.NhnService;
import no.ks.fiks.io.client.model.Konto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.client.ResourceAccessException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EntityServiceTest {

    @Mock
    private BrregService brregService;

    @Mock
    private ObjectProvider<FiksIoService> fiksIoService;

    @Mock
    private SRRequestScope requestScope;

    @Mock
    private NhnService nhnService;

    @InjectMocks
    private EntityService entityService;

    private static final String IDENTIFIER = "123456789";
    private static final String FIKS_IO_IDENTIFIER = "00000000-0000-0000-0000-000000000000";
    private static final String NHN_IDENTIFIER = "fastlege-for:17912099997";

    @BeforeEach
    void setUp() {
    }

    @Test
    void getEntityInfo_Iso6523Success() throws BrregNotFoundException {
        Iso6523 iso6523 = Iso6523.parse("0192:" + IDENTIFIER);
        OrganizationInfo orgInfo = new OrganizationInfo(IDENTIFIER, new OrganizationType("ORGL"));
        when(brregService.getOrganizationInfo(iso6523)).thenReturn(Optional.of(orgInfo));

        Optional<EntityInfo> result = entityService.getEntityInfo(iso6523);

        assertTrue(result.isPresent());
        assertEquals(orgInfo, result.get());
    }

    @Test
    void getEntityInfo_Iso6523NotFound() throws BrregNotFoundException {
        Iso6523 iso6523 = Iso6523.parse("0192:" + IDENTIFIER);
        when(brregService.getOrganizationInfo(iso6523)).thenThrow(new BrregNotFoundException("Not found"));

        Optional<EntityInfo> result = entityService.getEntityInfo(iso6523);

        assertTrue(result.isEmpty());
    }

    @Test
    void getEntityInfo_PersonIdentifier() {
        PersonIdentifier personIdentifier = PersonIdentifier.parse("17912099997");

        Optional<EntityInfo> result = entityService.getEntityInfo(personIdentifier);

        assertTrue(result.isPresent());
        assertInstanceOf(CitizenInfo.class, result.get());
        assertEquals("17912099997", result.get().getIdentifier());
    }

    @Test
    void getEntityInfo_FiksIoIdentifierSuccess() {
        FiksIoIdentifier fiksIoIdentifier = FiksIoIdentifier.parse(FIKS_IO_IDENTIFIER);
        FiksIoService fiksIoServiceMock = mock(FiksIoService.class);
        Konto konto = mock(Konto.class);

        when(fiksIoService.getIfAvailable()).thenReturn(fiksIoServiceMock);
        when(fiksIoServiceMock.lookup(FIKS_IO_IDENTIFIER)).thenReturn(Optional.of(konto));
        when(konto.isGyldigMottaker()).thenReturn(true);

        Optional<EntityInfo> result = entityService.getEntityInfo(fiksIoIdentifier);

        assertTrue(result.isPresent());
        assertInstanceOf(FiksIoInfo.class, result.get());
        assertEquals(FIKS_IO_IDENTIFIER, result.get().getIdentifier());
    }

    @Test
    void getEntityInfo_FiksIoIdentifierNotGyldig() {
        FiksIoIdentifier fiksIoIdentifier = FiksIoIdentifier.parse(FIKS_IO_IDENTIFIER);
        FiksIoService fiksIoServiceMock = mock(FiksIoService.class);
        Konto konto = mock(Konto.class);

        when(fiksIoService.getIfAvailable()).thenReturn(fiksIoServiceMock);
        when(fiksIoServiceMock.lookup(FIKS_IO_IDENTIFIER)).thenReturn(Optional.of(konto));
        when(konto.isGyldigMottaker()).thenReturn(false);

        Optional<EntityInfo> result = entityService.getEntityInfo(fiksIoIdentifier);

        assertTrue(result.isEmpty());
    }

    @Test
    void getEntityInfo_FiksIoServiceNotAvailable() {
        FiksIoIdentifier fiksIoIdentifier = FiksIoIdentifier.parse(FIKS_IO_IDENTIFIER);
        when(fiksIoService.getIfAvailable()).thenReturn(null);

        Optional<EntityInfo> result = entityService.getEntityInfo(fiksIoIdentifier);

        assertTrue(result.isEmpty());
    }

    @Test
    void getEntityInfo_NhnIdentifierSuccess() {
        NhnIdentifier nhnIdentifier = NhnIdentifier.parse(NHN_IDENTIFIER);
        HealthAddressRegistryDetails details = new HealthAddressRegistryDetails();
        details.setHerId(12345);
        details.setName("Test Health");
        details.setOrgNumber("999999999");

        when(nhnService.getARDetails(any())).thenReturn(details);

        Optional<EntityInfo> result = entityService.getEntityInfo(nhnIdentifier);

        assertTrue(result.isPresent());
        assertInstanceOf(HelseEnhetInfo.class, result.get());
        HelseEnhetInfo helseEnhetInfo = (HelseEnhetInfo) result.get();
        assertEquals(NHN_IDENTIFIER, helseEnhetInfo.getIdentifier());
        assertEquals(12345, helseEnhetInfo.getHerId());
    }

    @Test
    void getEntityInfo_NhnIdentifierNotFound() {
        NhnIdentifier nhnIdentifier = NhnIdentifier.parse(NHN_IDENTIFIER);
        when(nhnService.getARDetails(any())).thenThrow(new EntityNotFoundException("Not found"));

        Optional<EntityInfo> result = entityService.getEntityInfo(nhnIdentifier);

        assertTrue(result.isEmpty());
    }

    @Test
    void getEntityInfo_NhnIdentifierResourceAccessException() {
        NhnIdentifier nhnIdentifier = NhnIdentifier.parse(NHN_IDENTIFIER);
        when(nhnService.getARDetails(any())).thenThrow(new ResourceAccessException("Down"));

        Optional<EntityInfo> result = entityService.getEntityInfo(nhnIdentifier);

        assertTrue(result.isEmpty());
    }
}

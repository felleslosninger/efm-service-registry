package no.difi.meldingsutveksling.serviceregistry.record;

import no.difi.meldingsutveksling.serviceregistry.krr.PersonResource;
import org.junit.jupiter.api.Test;

import static no.difi.meldingsutveksling.serviceregistry.domain.Notification.NOT_OBLIGATED;
import static no.difi.meldingsutveksling.serviceregistry.domain.Notification.OBLIGATED;
import static no.difi.meldingsutveksling.serviceregistry.record.DpiMessageRouter.TargetRecord.*;
import static no.difi.meldingsutveksling.serviceregistry.record.DpiMessageRouter.route;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DpiMessageRouterTest {

    @Test
    public void routerShouldReturnDpiObligated() {
        PersonResource person = mock(PersonResource.class);
        when(person.isReserved()).thenReturn(false);
        when(person.isNotifiable()).thenReturn(true);
        when(person.isActive()).thenReturn(true);
        when(person.hasMailbox()).thenReturn(true);

        assertEquals(DPI, route(person, OBLIGATED));
    }

    @Test
    public void routerShouldReturnDpvObligated() {
        PersonResource person = mock(PersonResource.class);
        when(person.isReserved()).thenReturn(false);
        when(person.isNotifiable()).thenReturn(true);
        when(person.isActive()).thenReturn(false);
        when(person.hasMailbox()).thenReturn(false);

        assertEquals(DPV, route(person, OBLIGATED));
    }

    @Test
    public void routerShouldReturnPrintObligated() {
        PersonResource person = mock(PersonResource.class);
        when(person.isReserved()).thenReturn(true);
        when(person.isActive()).thenReturn(true);
        when(person.isNotifiable()).thenReturn(true);
        when(person.hasMailbox()).thenReturn(true);

        assertEquals(PRINT, route(person, OBLIGATED));
    }

    @Test
    public void routerShouldReturnDpiNotObligated() {
        PersonResource person = mock(PersonResource.class);
        when(person.isReserved()).thenReturn(true);
        when(person.isActive()).thenReturn(true);
        when(person.isNotifiable()).thenReturn(false);
        when(person.hasMailbox()).thenReturn(true);

        assertEquals(DPI, route(person, NOT_OBLIGATED));
    }

    @Test
    public void routerShouldReturnDpvNotObligated() {
        PersonResource person = mock(PersonResource.class);
        when(person.isReserved()).thenReturn(true);
        when(person.isActive()).thenReturn(false);
        when(person.isNotifiable()).thenReturn(false);
        when(person.hasMailbox()).thenReturn(true);

        assertEquals(DPV, route(person, NOT_OBLIGATED));
    }

}
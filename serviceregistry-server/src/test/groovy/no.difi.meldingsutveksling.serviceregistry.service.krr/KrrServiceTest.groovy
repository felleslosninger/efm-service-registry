package no.difi.meldingsutveksling.serviceregistry.service.krr

import no.difi.meldingsutveksling.ptp.KontaktInfo
import no.difi.meldingsutveksling.ptp.OppslagstjenesteClient
import no.difi.meldingsutveksling.ptp.PrintProviderDetails
import no.difi.meldingsutveksling.serviceregistry.config.ServiceregistryProperties
import no.difi.meldingsutveksling.serviceregistry.krr.LookupParameters
import spock.lang.Specification
import spock.lang.Unroll

import static org.mockito.Matchers.any
import static org.mockito.Mockito.*

class KrrServiceTest extends Specification {
    private OppslagstjenesteClient client
    private KontaktInfo kontaktInfo
    private krrService

    def setup() {
        client = mock(OppslagstjenesteClient)
        kontaktInfo = mock(KontaktInfo)
        krrService = new KrrService(new ServiceregistryProperties())
        krrService.setClient(this.client)
    }

    @Unroll
    def "KontaktInfo for #scenario then getCitizenInfo should setPrintDetails #print times"() {
        given:
            when(kontaktInfo.canReceiveDigitalPost()).thenReturn(receive)
            when(kontaktInfo.hasMailbox()).thenReturn(mailbox)
            when(this.client.hentKontaktInformasjon(any(LookupParameters.class))).thenReturn(kontaktInfo)

        when:
            krrService.getCitizenInfo(new LookupParameters("123456789"))
        then:
            verify(kontaktInfo, times(print)).setPrintDetails(any(PrintProviderDetails.class))
        where:
            scenario                                    | receive   | mailbox | print
            'can receive digital mail and has mailbox'  | true      | true    | 0
            'cannot receive digital mail'               | false     | true    | 1
            'can receive but does not have mailbox'     | true      | false   | 1
            'cannot receive but has mailbox'            | false     | true    | 1
    }
}

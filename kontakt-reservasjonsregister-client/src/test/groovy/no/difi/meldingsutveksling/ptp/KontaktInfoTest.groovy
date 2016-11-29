package no.difi.meldingsutveksling.ptp

import spock.lang.Specification
import spock.lang.Unroll

import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

class KontaktInfoTest extends Specification {
    private PersonKontaktInfoMapper.MailboxProvider mailboxProvider
    private PersonKontaktInfoMapper.PersonDetails personDetails


    def setup() {
        mailboxProvider = mock(PersonKontaktInfoMapper.MailboxProvider)
        personDetails = mock(PersonKontaktInfoMapper.PersonDetails)
    }

    @Unroll
    def "KontaktInfo with #scenario should be able to use digital mail #expected"() {
        given:
        when(mailboxProvider.hasMailbox()).thenReturn(mailbox)
        when(personDetails.isAktiv()).thenReturn(aktiv)
        when(personDetails.isReservert()).thenReturn(reservert)

        KontaktInfo ki = new KontaktInfo(mailboxProvider, personDetails)
        expect:
        //noinspection GrEqualsBetweenInconvertibleTypes
        ki.canReceiveDigitalPost() == expected
        where:
        scenario                    | mailbox   | aktiv | reservert | expected
        'missing mailbox'           | false     | true  | false     | false
        'person not active'         | true      | false | false     | false
        'person is reserved'        | true      | true  | true      | false
        'all required information'  | true      | true  | false     | true
    }
}
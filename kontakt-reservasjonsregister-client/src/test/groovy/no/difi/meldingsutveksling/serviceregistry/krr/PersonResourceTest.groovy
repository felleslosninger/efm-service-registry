package no.difi.meldingsutveksling.serviceregistry.krr

import spock.lang.Specification
import spock.lang.Unroll

import static org.mockito.Mockito.spy
import static org.mockito.Mockito.when

class PersonResourceTest extends Specification {
    private PersonResource personResource

    def setup() {
        personResource = spy(PersonResource)
    }

    @Unroll
    def "KontaktInfo with #scenario should be able to use digital mail #expected"() {
        given:
        when(personResource.hasMailbox()).thenReturn(mailbox)
        when(personResource.getStatus()).thenReturn(aktiv)
        when(personResource.getReserved()).thenReturn(reservert)

        expect:
        //noinspection GrEqualsBetweenInconvertibleTypes
        personResource.canReceiveDigitalPost() == expected

        where:
        scenario                    | mailbox   | aktiv     | reservert | expected
        'missing mailbox'           | false     | "AKTIV"   | "NEI"     | false
        'person not active'         | true      | "SLETTET" | "NEI"     | false
        'person is reserved'        | true      | "AKTIV"   | "JA"      | false
        'all required information'  | true      | "AKTIV"   | "NEI"     | true
    }
}
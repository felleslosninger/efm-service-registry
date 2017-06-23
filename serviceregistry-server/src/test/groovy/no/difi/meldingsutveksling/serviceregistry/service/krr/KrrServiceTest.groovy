package no.difi.meldingsutveksling.serviceregistry.service.krr

import no.difi.meldingsutveksling.serviceregistry.config.ServiceregistryProperties
import no.difi.meldingsutveksling.serviceregistry.krr.DSFClient
import no.difi.meldingsutveksling.serviceregistry.krr.DSFResource
import no.difi.meldingsutveksling.serviceregistry.krr.KRRClient
import no.difi.meldingsutveksling.serviceregistry.krr.PersonResource
import no.difi.move.common.oauth.KeystoreHelper
import spock.lang.Specification
import spock.lang.Unroll

import static no.difi.meldingsutveksling.Notification.NOT_OBLIGATED
import static no.difi.meldingsutveksling.Notification.OBLIGATED
import static no.difi.meldingsutveksling.serviceregistry.krr.LookupParameters.lookup

class KrrServiceTest extends Specification {
    public static final String someIdentifier = "12345"
    def service
    def personResource = Mock(PersonResource)
    def dsfResource = Mock(DSFResource)

    def setup() {
        def props = new ServiceregistryProperties()
        def krr = new ServiceregistryProperties.KontaktOgReservasjonsRegister()
        krr.setEndpointURL(new URL("http://foo"))
        krr.setDsfEndpointURL(new URL("http://foo"))
        props.setKrr(krr)
        service = Spy(KrrService, constructorArgs: [props, Mock(KeystoreHelper)])
    }

    @Unroll
    def "given recipient #scenario then KRRService should use print service"() {
        given:
        personResource.isNotifiable() >> isNotifiable
        personResource.canReceiveDigitalPost() >> true
        def lookupParameters = lookup(someIdentifier).require(notificationObligation).onBehalfOf(someIdentifier)
        def krrClient = Mock(KRRClient)
        krrClient.getPersonResource(_, _) >> personResource
        service.setKrrClient(krrClient)
        def dsfClient = Mock(DSFClient)
        dsfClient.getDSFResource(_, _) >> dsfResource
        service.setDsfClient(dsfClient)

        when:
        service.getCizitenInfo(lookupParameters)

        then:
        times * service.setPrintDetails(personResource)

        where:
        scenario                                                 | notificationObligation | isNotifiable | times
        "cannot be notified and is obligated to be notified"     | OBLIGATED              | false        | 1
        "can be notified and is not obligated to be notified"    | NOT_OBLIGATED          | true         | 0
        "cannot be notified and is not obligated to be notified" | NOT_OBLIGATED          | false        | 0
        "cannot be notified and is obligated to be notified"     | OBLIGATED              | true         | 0
    }

}

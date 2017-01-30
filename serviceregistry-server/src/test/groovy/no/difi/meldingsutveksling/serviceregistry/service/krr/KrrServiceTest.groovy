package no.difi.meldingsutveksling.serviceregistry.service.krr

import no.difi.meldingsutveksling.ptp.KontaktInfo
import no.difi.meldingsutveksling.ptp.OppslagstjenesteClient
import no.difi.meldingsutveksling.serviceregistry.config.ServiceregistryProperties
import spock.lang.Specification
import spock.lang.Unroll

import static no.difi.meldingsutveksling.Notification.NOT_OBLIGATED
import static no.difi.meldingsutveksling.Notification.OBLIGATED
import static no.difi.meldingsutveksling.serviceregistry.krr.LookupParameters.lookup

class KrrServiceTest extends Specification {
    public static final String someIdentifier = "12345"
    private KrrService service
    private kontaktInfo = Mock(KontaktInfo)

    def setup() {
        service = new KrrService(Mock(ServiceregistryProperties))
        this.service.client = Mock(OppslagstjenesteClient)
    }

    @Unroll
    def "given recipient #scenario then KRRService should use print service"() {
        given:
        kontaktInfo.isNotifiable() >> isNotifiable
        kontaktInfo.canReceiveDigitalPost() >> true
        def lookupParameters = lookup(someIdentifier).require(notificationObligation)
        service.client.hentKontaktInformasjon(lookupParameters) >> kontaktInfo

        when:
        service.getCitizenInfo(lookupParameters)

        then:
        times * kontaktInfo.setPrintDetails(_)

        where:
        scenario                                                 | notificationObligation | isNotifiable | times
        "cannot be notified and is obligated to be notified"     | OBLIGATED     | false        | 1
        "can be notified and is not obligated to be notified"    | NOT_OBLIGATED | true         | 0
        "cannot be notified and is not obligated to be notified" | NOT_OBLIGATED | false         | 0
        "cannot be notified and is obligated to be notified" | OBLIGATED     | true         | 0
    }
}

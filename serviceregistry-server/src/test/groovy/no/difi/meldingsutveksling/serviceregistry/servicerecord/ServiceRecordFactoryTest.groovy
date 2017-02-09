package no.difi.meldingsutveksling.serviceregistry.servicerecord

import no.difi.meldingsutveksling.Notification
import no.difi.meldingsutveksling.ptp.KontaktInfo
import no.difi.meldingsutveksling.serviceregistry.config.ServiceregistryProperties
import no.difi.meldingsutveksling.serviceregistry.exceptions.EndpointUrlNotFound
import no.difi.meldingsutveksling.serviceregistry.service.elma.ELMALookupService
import no.difi.meldingsutveksling.serviceregistry.service.krr.KrrService
import no.difi.meldingsutveksling.serviceregistry.service.ks.KSLookup
import no.difi.meldingsutveksling.serviceregistry.service.virksert.VirkSertService
import no.difi.vefa.peppol.common.lang.PeppolException
import no.difi.vefa.peppol.common.model.Endpoint
import spock.lang.Specification

class ServiceRecordFactoryTest extends Specification {
    private ServiceregistryProperties properties = Mock(ServiceregistryProperties)
    private VirkSertService virkSert = Mock(VirkSertService)
    private ELMALookupService elma = Mock(ELMALookupService)
    private KSLookup kSLookup = Mock(KSLookup)
    private KrrService krr
    private ServiceRecordFactory serviceRecordFactory


    def setup() {
        krr = Mock(KrrService)
        serviceRecordFactory = new ServiceRecordFactory(properties, virkSert, elma, krr)
    }

    def "Given citizen has not chosen postbox provider and citizen is not reserved then service record should be DPV"() {
        given:
        def kontaktInfo = Mock(KontaktInfo)
        kontaktInfo.isReservert() >> false
        kontaktInfo.hasMailbox() >> false
        krr.getCitizenInfo(_) >> kontaktInfo

        when:
        def serviceRecord = serviceRecordFactory.createServiceRecordForCititzen("1234", "4321", Notification.NOT_OBLIGATED)

        then:
        serviceRecord.class == PostVirksomhetServiceRecord
    }

    def "Given citizen can receive digital post then service record should be DPI"() {
        given:
        def kontaktInfo = Mock(KontaktInfo)
        kontaktInfo.hasMailbox() >> true
        krr.getCitizenInfo(_) >> kontaktInfo
        when:
        def serviceRecord = serviceRecordFactory.createServiceRecordForCititzen("1234", "4321", Notification.NOT_OBLIGATED)
        then:
        serviceRecord.class == SikkerDigitalPostServiceRecord
    }

    def "Given organization does not have integrasjonspunkt and is not in FIKS then service record should be DPV"() {
        given:
        elma.lookup(_) >> {throw new EndpointUrlNotFound("asdf", new PeppolException("asdf"))}

        when:
        def record = serviceRecordFactory.createEduServiceRecord("1234")

        then:
        record.class == PostVirksomhetServiceRecord
    }

    def "Given organization has a integrasjonspunkt"() {
        given:
        def endpoint = Mock(Endpoint)
        endpoint.address >> URI.create("")
        elma.lookup(_) >> endpoint

        when:
        def record = serviceRecordFactory.createEduServiceRecord("1234")

        then:
        record.class == EDUServiceRecord
    }
}

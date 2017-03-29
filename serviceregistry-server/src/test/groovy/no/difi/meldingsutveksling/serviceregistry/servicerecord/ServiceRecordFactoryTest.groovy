package no.difi.meldingsutveksling.serviceregistry.servicerecord

import no.difi.meldingsutveksling.Notification
import no.difi.meldingsutveksling.serviceregistry.config.ServiceregistryProperties
import no.difi.meldingsutveksling.serviceregistry.exceptions.EndpointUrlNotFound
import no.difi.meldingsutveksling.serviceregistry.krr.ContactInfoResource
import no.difi.meldingsutveksling.serviceregistry.krr.DSFResource
import no.difi.meldingsutveksling.serviceregistry.krr.DigitalPostResource
import no.difi.meldingsutveksling.serviceregistry.krr.PersonResource
import no.difi.meldingsutveksling.serviceregistry.service.elma.ELMALookupService
import no.difi.meldingsutveksling.serviceregistry.service.krr.KrrService
import no.difi.meldingsutveksling.serviceregistry.service.virksert.VirkSertService
import no.difi.vefa.peppol.common.lang.PeppolException
import no.difi.vefa.peppol.common.model.Endpoint
import spock.lang.Specification

class ServiceRecordFactoryTest extends Specification {
    private ServiceregistryProperties properties = Mock(ServiceregistryProperties)
    private VirkSertService virkSert = Mock(VirkSertService)
    private ELMALookupService elma = Mock(ELMALookupService)
    private KrrService krr
    private ServiceRecordFactory serviceRecordFactory


    def setup() {
        krr = Mock(KrrService)
        serviceRecordFactory = new ServiceRecordFactory(properties, virkSert, elma, krr)
    }

    def "Given citizen has not chosen postbox provider and citizen is not reserved then service record should be DPV"() {
        given:
        def personResourceMock = Mock(PersonResource)
        personResourceMock.getReserved() >> "NEI"
        personResourceMock.hasMailbox() >> false
        krr.getCizitenInfo(_) >> personResourceMock

        when:
        def serviceRecord = serviceRecordFactory.createServiceRecordForCititzen("1234", "4321", "1234", Notification.NOT_OBLIGATED)

        then:
        serviceRecord.class == PostVirksomhetServiceRecord
    }

    def "Given citizen can receive digital post then service record should be DPI"() {
        given:
        def personResourceMock = Mock(PersonResource)
        personResourceMock.hasMailbox() >> true
        personResourceMock.getDigitalPost() >> Mock(DigitalPostResource)
        personResourceMock.getContactInfo() >> Mock(ContactInfoResource)
        krr.getCizitenInfo(_) >> personResourceMock
        def dsfResourceMock = Mock(DSFResource)
        dsfResourceMock.getPostAddress() >> "foo bar"
        krr.getDSFInfo(_, _) >> dsfResourceMock

        when:
        def serviceRecord = serviceRecordFactory.createServiceRecordForCititzen("1234", "4321", "1234", Notification.NOT_OBLIGATED)

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

    def "Given organization has an integrasjonspunkt"() {
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

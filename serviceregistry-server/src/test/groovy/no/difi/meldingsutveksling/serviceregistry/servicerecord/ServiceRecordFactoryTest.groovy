package no.difi.meldingsutveksling.serviceregistry.servicerecord

import no.difi.meldingsutveksling.Notification
import no.difi.meldingsutveksling.serviceregistry.config.ServiceregistryProperties
import no.difi.meldingsutveksling.serviceregistry.exceptions.EndpointUrlNotFound
import no.difi.meldingsutveksling.serviceregistry.krr.ContactInfoResource
import no.difi.meldingsutveksling.serviceregistry.krr.DSFResource
import no.difi.meldingsutveksling.serviceregistry.krr.DigitalPostResource
import no.difi.meldingsutveksling.serviceregistry.krr.PersonResource
import no.difi.meldingsutveksling.serviceregistry.model.BrregPostadresse
import no.difi.meldingsutveksling.serviceregistry.model.OrganizationInfo
import no.difi.meldingsutveksling.serviceregistry.service.EntityService
import no.difi.meldingsutveksling.serviceregistry.service.elma.ELMALookupService
import no.difi.meldingsutveksling.serviceregistry.service.krr.KrrService
import no.difi.meldingsutveksling.serviceregistry.service.virksert.VirkSertService
import no.difi.vefa.peppol.common.lang.PeppolException
import no.difi.vefa.peppol.common.model.Endpoint
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetails
import spock.lang.Specification

class ServiceRecordFactoryTest extends Specification {
    private ServiceregistryProperties properties = Mock(ServiceregistryProperties)
    private VirkSertService virkSert = Mock(VirkSertService)
    private ELMALookupService elma = Mock(ELMALookupService)
    private EntityService entityService = Mock(EntityService)
    private KrrService krr
    private ServiceRecordFactory serviceRecordFactory


    def setup() {
        krr = Mock(KrrService)
        serviceRecordFactory = new ServiceRecordFactory(properties, virkSert, elma, krr, entityService)
    }

    def "Given citizen has not chosen postbox provider and citizen is not reserved then service record should be DPV"() {
        given:
        def personResourceMock = Mock(PersonResource)
        def auth = Mock(Authentication)
        def details = Mock(OAuth2AuthenticationDetails)
        details.getTokenValue() >> "4321"
        auth.getDetails() >> details
        personResourceMock.getReserved() >> "NEI"
        personResourceMock.hasMailbox() >> false
        krr.getCizitenInfo(_) >> personResourceMock

        when:
        def serviceRecord = serviceRecordFactory.createServiceRecordForCititzen("1234", auth, "1234", Notification.NOT_OBLIGATED)

        then:
        serviceRecord.get().class == PostVirksomhetServiceRecord
    }

    def "Given citizen can receive digital post then service record should be DPI"() {
        given:
        def personResourceMock = Mock(PersonResource)
        def auth = Mock(Authentication)
        def details = Mock(OAuth2AuthenticationDetails)
        details.getTokenValue() >> "4321"
        auth.getDetails() >> details
        personResourceMock.hasMailbox() >> true
        personResourceMock.getDigitalPost() >> Mock(DigitalPostResource)
        personResourceMock.getContactInfo() >> Mock(ContactInfoResource)
        def orginfoMock = Mock(OrganizationInfo)
        orginfoMock.getOrganizationName() >> "foo"
        def postadr = new BrregPostadresse("adr1", "123", "sted", "NOR")
        orginfoMock.getPostadresse() >> postadr
        entityService.getEntityInfo(_) >> Optional.of(orginfoMock)
        krr.getCizitenInfo(_) >> personResourceMock
        def dsfResourceMock = Mock(DSFResource)
        dsfResourceMock.getPostAddress() >> "foo bar"
        krr.getDSFInfo(_, _) >> Optional.of(dsfResourceMock)

        when:
        def serviceRecord = serviceRecordFactory.createServiceRecordForCititzen("1234", auth, "1234", Notification.NOT_OBLIGATED)

        then:
        serviceRecord.get().class == SikkerDigitalPostServiceRecord
    }

    def "Given organization does not have integrasjonspunkt and is not in FIKS then service record should be empty"() {
        given:
        elma.lookup(_) >> {throw new EndpointUrlNotFound("asdf", new PeppolException("asdf"))}

        when:
        def record = serviceRecordFactory.createEduServiceRecord("1234")

        then:
        !record.isPresent()
    }

    def "Given organization has an integrasjonspunkt"() {
        given:
        def endpoint = Mock(Endpoint)
        endpoint.address >> URI.create("")
        elma.lookup(_) >> endpoint

        when:
        def record = serviceRecordFactory.createEduServiceRecord("1234")

        then:
        record.get().class == EDUServiceRecord
    }
}

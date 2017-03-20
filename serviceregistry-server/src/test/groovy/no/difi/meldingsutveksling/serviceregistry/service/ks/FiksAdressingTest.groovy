package no.difi.meldingsutveksling.serviceregistry.service.ks

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4)
class FiksAdressingTest {

    @Test
    void "FiksAdressing empty does not have values"() {
        FiksAdressing fiksAdressing = FiksAdressing.EMPTY

        assert fiksAdressing == new FiksAdressing()
    }

    @Test
    void "given empty fiks adressing then should not use FIKS"() {
        FiksAdressing fiksAdressing = FiksAdressing.EMPTY

        assert fiksAdressing.shouldUseFIKS() == false
    }

    @Test
    void "given fiks adressing then should use FIKS"() {

        FiksAdressing fiksAdressing = new FiksAdressing(url: "http://localhost".toURL(), organizationId: "123456789", certificate: new PemCertificate(name: "test", value:"--certificate--"))

        assert fiksAdressing.shouldUseFIKS() == true
    }
}

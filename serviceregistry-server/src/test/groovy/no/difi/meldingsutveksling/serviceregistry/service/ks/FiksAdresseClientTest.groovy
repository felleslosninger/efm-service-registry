package no.difi.meldingsutveksling.serviceregistry.service.ks

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.client.ExpectedCount
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.web.client.RestTemplate

import static org.springframework.test.web.client.match.MockRestRequestMatchers.method
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess

@RunWith(JUnit4)
class FiksAdresseClientTest {
    FiksAdresseClient client
    private MockRestServiceServer server

    @Before
    void setup() {
        client = new FiksAdresseClient(new RestTemplate(), "http://localhost".toURL())
        this.server = MockRestServiceServer.createServer(client.getRestTemplate())

    }

    @Test
    void "given an organization on FIKS then client should get it's contactInfo"() {
        String identifier = "123456789"
        server.expect(ExpectedCount.once(), requestTo("http://localhost/organization/$identifier"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                """{
                  "organizationId": "$identifier",
                  "name": "Some organization",
                  "url": "http://localhost",
                  "certificate": {
                    "certificateId": 1,
                    "name": "Test sertifikat 2",
                    "value": "-----BEGIN CERTIFICATE-----\\r\\nMIID/DCCAuSgAwIBAgIED0vY/jANBgkqhkiG9w0BAQsFADBeMRIwEAYDVQQKEwlE\\r\\naWZpIHRlc3QxEjAQBgNVBAUTCTk5MTgyNTgyNzE0MDIGA1UEAxMrRElGSSB0ZXN0\\r\\nIHZpcmtzb21oZXRzc2VydGlmaWF0IGludGVybWVkaWF0ZTAeFw0xNTExMTYwODA0\\r\\nMjRaFw0xNzEyMTYwODA0MjVaMD0xEjAQBgNVBAUTCTk5MTgyNTgyNzEnMCUGA1UE\\r\\nAxMeRElGSSB0ZXN0IHZpcmtzb21oZXRzc2VydGlmaWF0MIIBIjANBgkqhkiG9w0B\\r\\nAQEFAAOCAQ8AMIIBCgKCAQEA1XagPmTItqUK/5gaw7n/UodGfI0Ez0XEFUXj2k9C\\r\\nAnvpa/k/vj0jNSs9yACVC1MDNSd9aWDzuiz2qwQC8cGuQgX//MKI4XFci2VRm/C1\\r\\nMz4NiMpNoygxcXI7PheJgqdMx6Oh/W+NoVDDo8vkxj8m+gom8kURtIzvFLxNpDv1\\r\\nCQRDyZk01yhuw676+IIaXCWMXKdG3VyzfxGYxZxosHe+VQaU/IplEP7y7LOwKDXC\\r\\n0VMwZotXJ1lMaRVulGt2lEkK4+NBd2lbTGyveWYjSazB+IhsYuV5N+KfFBhtpx8n\\r\\nWgaGy647BgFpPJAt2pjvLcBRVDu83bjvE6tvj1bfHiKu/wIDAQABo4HiMIHfMIGL\\r\\nBgNVHSMEgYMwgYCAFCeuypqN1OjDIoi4bYBAcNfT83GzoWKkYDBeMRIwEAYDVQQK\\r\\nEwlEaWZpIHRlc3QxEjAQBgNVBAUTCTk5MTgyNTgyNzE0MDIGA1UEAxMrRElGSSB0\\r\\nZXN0IHZpcmtzb21oZXRzc2VydGlmaWF0IGludGVybWVkaWF0ZYIEJoRZSzAdBgNV\\r\\nHQ4EFgQULJ+KXWYIvJXxLjMwSWaGu5MyQlgwCQYDVR0TBAIwADAVBgNVHSAEDjAM\\r\\nMAoGCGCEQgEBAQFkMA4GA1UdDwEB/wQEAwIEsDANBgkqhkiG9w0BAQsFAAOCAQEA\\r\\nClIxJDOj7s5Mddfhn1QoBtb8sOYk5rVFHTP8WnX65+vtjax5iLoDY9g3GwQ35a/x\\r\\nAqYqV1Eqc0vsOrxz0BcUVSocraPL6Jth/H6ff+y4yJ73Ee0WeVLX97c/g+fM8pJc\\r\\n6G6/oK0bJGt+1Pui4HEu3SNpl0RU9PZLPT307gFd0BamSvAp56+oXLDnD9Z8TpG4\\r\\niAJcbitSgYTpMGBeJeoXhlHPH9JSJygYLYHr6NOYHzp+rrOD/4NDKSMSzESB97Lv\\r\\nU07yObeD2GdoZHobzmckgpE6400XgzA22mbpRNlDURfBCK3vC6qHOSE5PP7zXByh\\r\\nWHa5xZRh3lUuAHR9ZOzYpw==\\r\\n-----END CERTIFICATE-----\\r\\n"
                  }
                }"""
                , MediaType.APPLICATION_JSON))

        def organization = client.getOrganization("123456789")

        server.verify()
        organization != FiksContactInfo.EMPTY
    }

    @Test
    void "given an organization that does not exist on FIKS then client should return empty organization"() {
        String identifier = "987654321"
        server.expect(ExpectedCount.once(), requestTo("http://localhost/organization/$identifier"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withStatus(HttpStatus.NOT_FOUND))

        def contactInfo = client.getOrganization(identifier)

        server.verify()
        contactInfo == FiksContactInfo.EMPTY
    }
}

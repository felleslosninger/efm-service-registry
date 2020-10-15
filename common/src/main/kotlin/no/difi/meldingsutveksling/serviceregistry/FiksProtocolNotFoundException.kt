package no.difi.meldingsutveksling.serviceregistry

import java.lang.Exception

class FiksProtocolNotFoundException(identifier: String) : Exception("No mapping to Fiks IO protocol found for identifier $identifier")
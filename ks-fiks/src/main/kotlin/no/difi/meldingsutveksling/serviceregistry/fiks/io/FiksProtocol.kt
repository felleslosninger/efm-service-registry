package no.difi.meldingsutveksling.serviceregistry.fiks.io

import no.difi.meldingsutveksling.serviceregistry.domain.Process
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import javax.persistence.*

@Entity
@Table(uniqueConstraints = [UniqueConstraint(columnNames = ["identifier"])])
class FiksProtocol(
        @Id
        @GeneratedValue
        var id: Long? = null,

        var identifier: String,

        @OneToMany
        var processes: MutableSet<Process> = mutableSetOf()
)

@Repository
interface FiksProtocolRepository : CrudRepository<FiksProtocol, Long> {
        fun findByProcessesIdentifier(identifier: String): FiksProtocol?
        fun findByIdentifier(identifier: String): FiksProtocol?
        fun deleteByIdentifier(identifier: String)
        fun existsByIdentifier(identifier: String): Boolean
}

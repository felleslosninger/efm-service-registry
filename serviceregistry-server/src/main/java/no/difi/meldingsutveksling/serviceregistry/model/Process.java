package no.difi.meldingsutveksling.serviceregistry.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.ToString;
import no.difi.vefa.peppol.common.model.ProcessIdentifier;

import javax.persistence.Entity;
import javax.persistence.*;
import java.util.List;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = "identifier"))
@Data
@ToString(exclude = "documentTypes")
public class Process {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    private String identifier;
    private String serviceCode;
    private String serviceEditionCode;

    @Enumerated(EnumType.STRING)
    private ProcessCategory category;

    @ManyToMany
    @JoinTable(
            name = "process_document_type",
            joinColumns = @JoinColumn(name = "proc_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "doctype_id", referencedColumnName = "id")
    )
    @JsonIgnoreProperties("processes")
    private List<DocumentType> documentTypes;

}

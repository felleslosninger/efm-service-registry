package no.difi.meldingsutveksling.serviceregistry.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import jakarta.persistence.Entity;
import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = "identifier"))
@Data
@ToString(exclude = "processes")
@EqualsAndHashCode(exclude = "processes")
@NoArgsConstructor
public class DocumentType {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @JsonIgnore
    private long id;

    private String identifier;

    @ManyToMany(mappedBy = "documentTypes", fetch = FetchType.LAZY)
    @JsonIgnoreProperties("documentTypes")
    private List<Process> processes;

}


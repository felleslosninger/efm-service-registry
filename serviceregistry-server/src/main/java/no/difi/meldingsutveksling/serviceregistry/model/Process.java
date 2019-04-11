package no.difi.meldingsutveksling.serviceregistry.model;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import lombok.Data;
import lombok.ToString;

import javax.persistence.Entity;
import javax.persistence.*;
import java.util.List;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = "identifier"))
@Data
@ToString(exclude = "documentTypes")
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class Process {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    private String identifier;
    private String serviceCode;
    private String serviceEditionCode;

    @ManyToMany
    private List<DocumentType> documentTypes;

}

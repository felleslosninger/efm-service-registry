package no.difi.meldingsutveksling.serviceregistry.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import javax.persistence.*;
import javax.persistence.Entity;

@Data
@RequiredArgsConstructor(staticName = "of")
@NoArgsConstructor
@Entity
@Table(name = "brreg_mock_enhet",
        indexes = {
                @Index(columnList = "orgnr")
        })
public class BrregMockEnhet {

    @Id
    @Column(name = "orgnr", length = 10)
    @NonNull
    private String orgnr;
    @NonNull
    private String name;
    @NonNull
    private String orgform;
}

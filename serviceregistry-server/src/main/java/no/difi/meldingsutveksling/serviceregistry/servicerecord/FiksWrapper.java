package no.difi.meldingsutveksling.serviceregistry.servicerecord;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor(staticName = "of")
public class FiksWrapper {

    private ServiceRecord serviceRecord;
    private Integer securitylevel;
}

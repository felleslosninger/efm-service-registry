package no.difi.meldingsutveksling.serviceregistry.servicerecord;

import no.difi.meldingsutveksling.serviceregistry.model.ServiceIdentifier;
import no.ks.fiks.io.client.model.Konto;

import java.util.List;

public class DpfioServiceRecord extends ServiceRecord {

    private DpfioServiceRecord(ServiceIdentifier serviceIdentifier, String organisationNumber, String kontoId) {
        super(serviceIdentifier, organisationNumber, kontoId);
    }

    public static DpfioServiceRecord from(String orgnr,
                                        Konto kontoId,
                                        String processIdentifier,
                                        List<String> documentTypes) {
        DpfioServiceRecord dpfioServiceRecord = new DpfioServiceRecord(ServiceIdentifier.DPFIO,
                orgnr,
                kontoId.getKontoId().toString());
        dpfioServiceRecord.setProcess(processIdentifier);
        dpfioServiceRecord.setDocumentTypes(documentTypes);
        return dpfioServiceRecord;
    }

}

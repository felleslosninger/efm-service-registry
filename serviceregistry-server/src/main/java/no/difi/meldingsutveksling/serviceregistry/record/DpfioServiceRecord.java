package no.difi.meldingsutveksling.serviceregistry.record;

import no.difi.meldingsutveksling.serviceregistry.domain.ServiceIdentifier;
import no.ks.fiks.io.client.model.Konto;

import java.util.List;

public class DpfioServiceRecord extends ServiceRecord {

    private DpfioServiceRecord(ServiceIdentifier serviceIdentifier, String organisationNumber, String kontoId) {
        super(serviceIdentifier, organisationNumber, kontoId);
    }

    public static DpfioServiceRecord from(String orgnr,
                                        Konto kontoId,
                                        String processIdentifier,
                                        String fiksProtocol,
                                        List<String> documentTypes) {
        DpfioServiceRecord dpfioServiceRecord = new DpfioServiceRecord(ServiceIdentifier.DPFIO,
                orgnr,
                kontoId.getKontoId().toString());
        dpfioServiceRecord.getService().setServiceCode(fiksProtocol);
        dpfioServiceRecord.setProcess(processIdentifier);
        dpfioServiceRecord.setDocumentTypes(documentTypes);
        return dpfioServiceRecord;
    }

}

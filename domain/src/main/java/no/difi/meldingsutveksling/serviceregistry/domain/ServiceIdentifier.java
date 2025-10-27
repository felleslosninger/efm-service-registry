package no.difi.meldingsutveksling.serviceregistry.domain;

/**
 * Identifiers for the individual services available for transport of messages
 */
public enum ServiceIdentifier {
    /**
     * Identifies archive-to-archive transportation
     */
    DPO,
    /**
     * Identifies transport til EPJ(fastlege)
     */
    DPH,
    /**
     * Identifies using Altinn correspondence agency as transport
     */
    DPV,
    DPF,
     /**
     * Identifies DIFI sikker digital post aka. Digital Post Innbygger
     */
    DPI,
    /**
     * Identifies eInnsyn
     */
    DPE,
    /**
     * Fiks IO
     */
    DPFIO
}

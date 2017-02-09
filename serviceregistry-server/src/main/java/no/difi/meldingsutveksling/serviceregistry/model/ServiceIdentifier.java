package no.difi.meldingsutveksling.serviceregistry.model;

/**
 * Identifiers for the individual services available for transport of messages
 */
public enum ServiceIdentifier {
    /**
     * Identifies archive-to-archive transportation
     */
    EDU,
    /**
     * Identifies using Altinn correspondence agency as transport
     */
    POST_VIRKSOMHET,
    FIKS, /**
     * Identifies DIFI sikker digital post aka. Digital Post Innbygger
     */
    DPI,
    DPE_innsyn,
    DPE_data
}

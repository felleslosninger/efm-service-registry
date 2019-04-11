package no.difi.meldingsutveksling.serviceregistry.web;

import no.difi.meldingsutveksling.serviceregistry.model.DocumentType;
import no.difi.meldingsutveksling.serviceregistry.model.Process;
import no.difi.meldingsutveksling.serviceregistry.service.DocumentTypeService;
import no.difi.meldingsutveksling.serviceregistry.service.ProcessService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class AdminController {

    private final ProcessService processService;
    private final DocumentTypeService documentTypeService;

    public AdminController(ProcessService processService, DocumentTypeService documentTypeService) {
        this.processService = processService;
        this.documentTypeService = documentTypeService;
    }

    @GetMapping(value = "/processes/{identifier:.+}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Process> getProcess(@PathVariable String identifier) {
        Process process = processService.findByIdentifier(identifier);
        return null != process
                ? ResponseEntity.ok(process)
                : ResponseEntity.notFound().build();
    }

    @PostMapping(value = "/processes", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> addProcess(@RequestBody Process process) {
        try {
            Process existingProcess = processService.findByIdentifier(process.getIdentifier());
            if (null != existingProcess) {
                return ResponseEntity.status(HttpStatus.CONFLICT).build();
            }
            processService.add(process);
            UriComponents uriComponents = UriComponentsBuilder.fromUriString("/processes")
                    .path(process.getIdentifier())
                    .build();
            return ResponseEntity.created(uriComponents.toUri()).build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping(value = "/processes/{identifier:.+}")
    public ResponseEntity<?> deleteProcess(@PathVariable String identifier) {
        try {
            Process process = processService.findByIdentifier(identifier);
            if (process != null) {
                processService.delete(process);
                return ResponseEntity.noContent().build();
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping(value = "/processes", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Process>> getProcesses() {
        try {
            return ResponseEntity.ok(processService.findAll());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @PutMapping(value = "/processes/{processIdentifier:.+}/documentTypes", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> addDocumentTypes(@PathVariable String processIdentifier, @RequestBody List<String> documentTypeIdentifiers) {
        try {
            Process process = processService.findByIdentifier(processIdentifier);
            if (null == process) {
                return ResponseEntity.notFound().build();
            }
            List<DocumentType> documentTypes = new ArrayList<>();
            for (String identifier : documentTypeIdentifiers) {
                DocumentType documentType = documentTypeService.findByIdentifier(identifier);
                if (null == documentType) {
                    documentType = new DocumentType();
                    documentType.setIdentifier(identifier);
                    documentTypeService.add(documentType);
                }
                documentTypes.add(documentType);
            }
            process.setDocumentTypes(documentTypes);
            processService.update(process);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping(value = "/documentTypes/{identifier}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DocumentType> getDocumentType(@PathVariable String identifier) {
        DocumentType documentType = documentTypeService.findByIdentifier(identifier);
        return null != documentType
                ? ResponseEntity.ok(documentType)
                : ResponseEntity.notFound().build();
    }

    @PostMapping(value = "/documentTypes", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> addDocumentType(@RequestBody DocumentType documentType) {
        try {
            DocumentType existingDocumentType = documentTypeService.findByIdentifier(documentType.getIdentifier());
            if (null != existingDocumentType) {
                return ResponseEntity.status(HttpStatus.CONFLICT).build();
            }
            documentTypeService.add(documentType);
            UriComponents uriComponents = UriComponentsBuilder.fromUriString("/documentType")
                    .path(documentType.getIdentifier())
                    .build();
            return ResponseEntity.created(uriComponents.toUri()).build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping(value = "/documentTypes/{identifier:.+}")
    public ResponseEntity<?> deleteDocumentType(@PathVariable String identifier) {
        try {
            DocumentType documentType = documentTypeService.findByIdentifier(identifier);
            if (documentType != null) {
                documentTypeService.delete(documentType);
                return ResponseEntity.noContent().build();
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping(value = "/documentTypes", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<DocumentType>> getDocumentTypes() {
        try {
            return ResponseEntity.ok(documentTypeService.findAll());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
}

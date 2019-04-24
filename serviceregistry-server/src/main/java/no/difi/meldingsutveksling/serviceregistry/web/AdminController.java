package no.difi.meldingsutveksling.serviceregistry.web;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.serviceregistry.model.DocumentType;
import no.difi.meldingsutveksling.serviceregistry.model.Process;
import no.difi.meldingsutveksling.serviceregistry.service.DocumentTypeService;
import no.difi.meldingsutveksling.serviceregistry.service.ProcessService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1")
@Slf4j
public class AdminController {

    private final ProcessService processService;
    private final DocumentTypeService documentTypeService;

    public AdminController(ProcessService processService, DocumentTypeService documentTypeService) {
        this.processService = processService;
        this.documentTypeService = documentTypeService;
    }

    @GetMapping(value = "/processes/{identifier:.+}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Process> getProcess(@PathVariable String identifier) {
        Optional<Process> process = processService.findByIdentifier(identifier);
        return process.isPresent()
                ? ResponseEntity.ok(process.get())
                : ResponseEntity.notFound().build();
    }

    @PostMapping(value = "/processes", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public ResponseEntity<?> addProcess(@RequestBody Process process) {
        try {
            Optional<Process> existingProcess = processService.findByIdentifier(process.getIdentifier());
            if (existingProcess.isPresent()) {
                return ResponseEntity.status(HttpStatus.CONFLICT).build();
            }
            List<DocumentType> persistedDoctypes = Lists.newArrayList();
            for (DocumentType type : process.getDocumentTypes()) {
                Optional<DocumentType> docFind = documentTypeService.findByIdentifier(type.getIdentifier());
                if (!docFind.isPresent()) {
                    persistedDoctypes.add(documentTypeService.add(type));
                } else {
                    persistedDoctypes.add(docFind.get());
                }
            }
            process.setDocumentTypes(persistedDoctypes);
            processService.add(process);
            UriComponents uriComponents = UriComponentsBuilder.fromUriString("/processes")
                    .path(process.getIdentifier())
                    .build();
            return ResponseEntity.created(uriComponents.toUri()).build();
        } catch (Exception e) {
            log.error("Exception during process save", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping(value = "/processes/{identifier:.+}")
    public ResponseEntity<?> deleteProcess(@PathVariable String identifier) {
        try {
            Optional<Process> process = processService.findByIdentifier(identifier);
            if (process.isPresent()) {
                processService.delete(process.get());
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
            Optional<Process> optionalProcess = processService.findByIdentifier(processIdentifier);
            if (!optionalProcess.isPresent()) {
                return ResponseEntity.notFound().build();
            }
            Process process = optionalProcess.get();
            List<DocumentType> documentTypes = new ArrayList<>();
            for (String identifier : documentTypeIdentifiers) {
                Optional<DocumentType> existingDocumentType = documentTypeService.findByIdentifier(identifier);
                DocumentType documentType;
                if (existingDocumentType.isPresent()) {
                    documentType = existingDocumentType.get();
                } else {
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
    @PutMapping(value = "/processes/{processIdentifier:.+}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> updateProcess(@PathVariable String processIdentifier, @RequestBody Process processWithValuesForUpdate) {
        try {
            if (processService.updateProcess(processIdentifier, processWithValuesForUpdate)) {
                return ResponseEntity.ok().build();
            }
            else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }


    @GetMapping(value = "/documentTypes/{identifier}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DocumentType> getDocumentType(@PathVariable String identifier) {
        Optional<DocumentType> documentType = documentTypeService.findByIdentifier(identifier);
        return documentType.isPresent()
                ? ResponseEntity.ok(documentType.get())
                : ResponseEntity.notFound().build();
    }

    @PostMapping(value = "/documentTypes", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> addDocumentType(@RequestBody DocumentType documentType) {
        try {
            Optional<DocumentType> existingDocumentType = documentTypeService.findByIdentifier(documentType.getIdentifier());
            if (existingDocumentType.isPresent()) {
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
            Optional<DocumentType> documentType = documentTypeService.findByIdentifier(identifier);
            if (documentType.isPresent()) {
                documentTypeService.delete(documentType.get());
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

package no.difi.meldingsutveksling.serviceregistry.controller;

import com.google.common.collect.Lists;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.serviceregistry.domain.DocumentType;
import no.difi.meldingsutveksling.serviceregistry.domain.Process;
import no.difi.meldingsutveksling.serviceregistry.service.DocumentTypeService;
import no.difi.meldingsutveksling.serviceregistry.service.ProcessService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Optional;

@Api(value = "Administration", tags = {"Administration"})
@RestController
@RequestMapping("/api/v1")
@Slf4j
@RequiredArgsConstructor
public class AdminController {

    private final ProcessService processService;
    private final DocumentTypeService documentTypeService;


    @GetMapping(value = "/processes/{identifier:.+}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Process> getProcess(@ApiParam("Process identifier") @PathVariable String identifier) {
        Optional<Process> process = processService.findByIdentifier(identifier);
        return process.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping(value = "/processes/list", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public ResponseEntity<?> addProcesses(@RequestBody List<Process> processes) {
        processes.forEach(this::addProcess);
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/processes", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public ResponseEntity<?> addProcess(@ApiParam("Process data") @RequestBody Process process) {
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
    public ResponseEntity<?> deleteProcess(@ApiParam("Process identifier") @PathVariable String identifier) {
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

    @PutMapping(value = "/processes/{processIdentifier:.+}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> updateProcess(@ApiParam("Process identifier") @PathVariable String processIdentifier,
                                        @ApiParam("Process data") @RequestBody Process processWithValuesForUpdate) {
        try {
            if (processService.update(processIdentifier, processWithValuesForUpdate)) {
                return ResponseEntity.ok().build();
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping(value = "/documentTypes/{identifier}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DocumentType> getDocumentType(@ApiParam("Document type identifier") @PathVariable String identifier) {
        Optional<DocumentType> documentType = documentTypeService.findByIdentifier(identifier);
        return documentType.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping(value = "/documentTypes", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> addDocumentType(@ApiParam("Document type data") @RequestBody DocumentType documentType) {
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
    public ResponseEntity<?> deleteDocumentType(@ApiParam("Document type identifier") @PathVariable String identifier) {
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

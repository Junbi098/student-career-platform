package com.studentcareer.web.controller;

import com.studentcareer.opportunity.model.OpportunityInput;
import com.studentcareer.opportunity.model.OpportunityTypes.*;
import com.studentcareer.opportunity.persistence.OpportunityReviewEntity.ReviewRecord;
import com.studentcareer.opportunity.service.*;
import com.studentcareer.opportunity.service.OpportunityContracts.*;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/opportunities")
public class OpportunityController {
    private final OpportunityService service;
    private final JobPostingImportService imports;

    public OpportunityController(OpportunityService service, JobPostingImportService imports) {
        this.service = service;
        this.imports = imports;
    }
    @PostMapping
    public ResponseEntity<View> create(@RequestBody OpportunityInput input) {
        View created = service.create(input);
        return ResponseEntity.created(URI.create("/api/opportunities/" + created.id())).body(created);
    }
    @PutMapping("/{id}")
    public View update(@PathVariable UUID id, @RequestBody Update input) { return service.update(id, input); }
    @GetMapping("/{id}")
    public View find(@PathVariable UUID id) { return service.find(id); }
    @PostMapping("/{id}/review")
    public View review(@PathVariable UUID id, @RequestBody Review request) { return service.review(id, request); }
    @GetMapping("/{id}/reviews")
    public List<ReviewRecord> reviews(@PathVariable UUID id) { return service.history(id); }
    @PostMapping("/imports/html")
    public ImportReport html(@RequestBody HtmlImport input) { return imports.ingest(input); }
    @GetMapping
    public Page search(@RequestParam(required=false) String q, @RequestParam(required=false) String sector,
            @RequestParam(required=false) String location, @RequestParam(required=false) Type type,
            @RequestParam(required=false) WorkMode workMode, @RequestParam(required=false) StudentType studentType,
            @RequestParam(defaultValue="false") boolean includeUnknownEligibility,
            @RequestParam(required=false) Status status, @RequestParam(defaultValue="false") boolean verifiedOnly,
            @RequestParam(defaultValue="false") boolean includeInactive, @RequestParam(defaultValue="0") int page,
            @RequestParam(defaultValue="20") int size, @RequestParam(defaultValue="RELEVANCE") Sort sort) {
        return service.search(new Query(q,sector,location,type,workMode,studentType,includeUnknownEligibility,
                status,verifiedOnly,includeInactive,page,size,sort));
    }
}

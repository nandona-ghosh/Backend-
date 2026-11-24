package com.foodplant.api;
import jakarta.validation.Valid; import jakarta.validation.constraints.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.data.domain.*; import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity; import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
@io.swagger.v3.oas.annotations.security.SecurityRequirement(name="bearerAuth")
@RestController @Validated @RequestMapping("/api/v1/batches")
class BatchController {
 private final BatchService service; BatchController(BatchService service) { this.service=service; }
 @GetMapping PageResponse<Batch> list(@RequestParam(required=false) @Size(max=64) String number, @RequestParam(required=false) @Positive Long lineId, @RequestParam(required=false) @Pattern(regexp="PENDING|HOLD|RELEASED|REJECTED") String status, @RequestParam(required=false) java.time.LocalDate from, @RequestParam(required=false) java.time.LocalDate to, @RequestParam(defaultValue="0") @Min(0) @Max(10000) int page, @RequestParam(defaultValue="20") @Min(1) @Max(100) int size) {
    if(from != null && to != null && from.isAfter(to)) throw new InvalidException("Start date must not be after end date");
    Specification<Batch> filter=(root,query,cb) -> { var predicates=new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
        if(number!=null && !number.isBlank()) predicates.add(cb.like(cb.lower(root.get("batchNumber")), "%"+number.toLowerCase(java.util.Locale.ROOT).replace("%","\\%").replace("_","\\_")+"%",'\\'));
        if(lineId!=null) predicates.add(cb.equal(root.get("lineId"),lineId));
        if(status!=null) predicates.add(cb.equal(root.get("status"),status));
        if(from!=null) predicates.add(cb.greaterThanOrEqualTo(root.get("productionDate"),from));
        if(to!=null) predicates.add(cb.lessThanOrEqualTo(root.get("productionDate"),to));
        return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0])); };
    return PageResponse.of(service.list(filter,PageRequest.of(page,size,Sort.by("id")))); }
 @GetMapping("/{id}") Batch get(@PathVariable @Positive long id) { return service.get(id); }
 @PostMapping ResponseEntity<Batch> create(@Valid @RequestBody BatchRequest request) { Batch result=service.create(request); return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(result.id).toUri()).body(result); }
 @PutMapping("/{id}") Batch update(@PathVariable @Positive long id,@Valid @RequestBody BatchRequest request) { return service.update(id,request); }
 @DeleteMapping("/{id}") ResponseEntity<Void> delete(@PathVariable @Positive long id) { service.delete(id); return ResponseEntity.noContent().build(); }
}

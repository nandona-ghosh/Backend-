package com.foodplant.api;
import jakarta.validation.Valid; import jakarta.validation.constraints.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.data.domain.*; import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity; import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
@io.swagger.v3.oas.annotations.security.SecurityRequirement(name="bearerAuth")
@RestController @Validated @RequestMapping("/api/v1/product-lines")
class ProductLineController {
 private final ProductLineService service; ProductLineController(ProductLineService service) { this.service=service; }
 @GetMapping PageResponse<ProductLine> list(@RequestParam(required=false) @Size(max=120) String search, @RequestParam(defaultValue="0") @Min(0) @Max(10000) int page, @RequestParam(defaultValue="20") @Min(1) @Max(100) int size) {
    Specification<ProductLine> filter=(root,query,cb) -> search==null || search.isBlank() ? cb.conjunction() : cb.like(cb.lower(root.get("name")), "%"+search.toLowerCase(java.util.Locale.ROOT).replace("%","\\%").replace("_","\\_")+"%",'\\');
    return PageResponse.of(service.list(filter,PageRequest.of(page,size,Sort.by("id")))); }
 @GetMapping("/{id}") ProductLine get(@PathVariable @Positive long id) { return service.get(id); }
 @PostMapping ResponseEntity<ProductLine> create(@Valid @RequestBody ProductLineRequest request) { ProductLine result=service.create(request); return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(result.id).toUri()).body(result); }
 @PutMapping("/{id}") ProductLine update(@PathVariable @Positive long id,@Valid @RequestBody ProductLineRequest request) { return service.update(id,request); }
 @DeleteMapping("/{id}") ResponseEntity<Void> delete(@PathVariable @Positive long id) { service.delete(id); return ResponseEntity.noContent().build(); }
}

package com.foodplant.api;
import java.util.List; import jakarta.validation.constraints.*; import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
@io.swagger.v3.oas.annotations.security.SecurityRequirement(name="bearerAuth")
@RestController @Validated @RequestMapping("/api/v1") class TraceController {
 private final TraceService service; TraceController(TraceService service) {this.service=service;}
 @GetMapping("/batches/{id}/trace") TraceService.Trace trace(@PathVariable @Positive long id) {return service.trace(id);}
 @GetMapping("/machines") List<TraceService.Machine> machines(@RequestParam(defaultValue="0") @Min(0) @Max(10000) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size) {return service.machines(page,size);}
 @GetMapping("/inventory/material-lots") List<TraceService.Inventory> inventory(@RequestParam(defaultValue="0") @Min(0) @Max(10000) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size) {return service.inventory(page,size);}
}

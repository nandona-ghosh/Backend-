package com.foodplant.api;
import jakarta.validation.Valid; import jakarta.validation.constraints.*; import java.math.BigDecimal; import java.time.*;
import org.springframework.http.ResponseEntity; import org.springframework.validation.annotation.Validated; import org.springframework.web.bind.annotation.*;
@io.swagger.v3.oas.annotations.security.SecurityRequirement(name="bearerAuth")
@RestController @Validated @RequestMapping("/api/v1") class TraceWriteController {
 private final TraceWriteService service; TraceWriteController(TraceWriteService service){this.service=service;}
 public record IdResponse(long id) {}
 public record LotRequest(@NotNull @Positive Long supplierId,@NotBlank @Size(max=120) String materialName,@NotBlank @Size(max=64) String lotNumber,@NotNull @DecimalMin("0.001") @Digits(integer=9,fraction=3) BigDecimal receivedKg,@NotNull LocalDate receivedDate) {}
 public record ConsumeRequest(@NotNull @Positive Long materialLotId,@NotNull @DecimalMin("0.001") @Digits(integer=9,fraction=3) BigDecimal usedKg) {}
 public record QualityRequest(@NotNull Instant sampledAt,@NotNull Instant recordedAt,@NotBlank @Pattern(regexp="PASS|FAIL|PENDING") String result,@DecimalMin("0.00") @DecimalMax("100.00") @Digits(integer=3,fraction=2) BigDecimal moisturePercent,@Size(max=300) String note) {}
 public record RunRequest(@NotNull @Positive Long machineId,@NotNull Instant startedAt,@NotNull Instant endedAt) {}
 @PostMapping("/inventory/material-lots") ResponseEntity<IdResponse> lot(@Valid @RequestBody LotRequest r){return ResponseEntity.status(201).body(new IdResponse(service.addLot(r)));}
 @PostMapping("/batches/{id}/materials") ResponseEntity<IdResponse> consume(@PathVariable @Positive long id,@Valid @RequestBody ConsumeRequest r){return ResponseEntity.status(201).body(new IdResponse(service.consume(id,r)));}
 @PostMapping("/batches/{id}/quality-tests") ResponseEntity<IdResponse> quality(@PathVariable @Positive long id,@Valid @RequestBody QualityRequest r){return ResponseEntity.status(201).body(new IdResponse(service.quality(id,r)));}
 @PostMapping("/batches/{id}/machine-runs") ResponseEntity<IdResponse> run(@PathVariable @Positive long id,@Valid @RequestBody RunRequest r){return ResponseEntity.status(201).body(new IdResponse(service.machineRun(id,r)));}
}

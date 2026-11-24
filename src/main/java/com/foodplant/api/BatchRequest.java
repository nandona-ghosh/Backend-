package com.foodplant.api;
import jakarta.validation.constraints.*;
public record BatchRequest(@NotBlank @Size(max=64) String batchNumber, @NotBlank @Size(max=32) String numberingEpoch, @NotNull @Positive Long lineId, @NotNull java.time.LocalDate productionDate, @NotBlank @Pattern(regexp="DAY|NIGHT") String shift, @NotBlank @Pattern(regexp="PENDING|HOLD|RELEASED|REJECTED") String status, @NotNull @DecimalMin(value="0.001") @Digits(integer=9, fraction=3) java.math.BigDecimal quantityKg) {}

package com.foodplant.api;
import jakarta.validation.constraints.*;
public record SupplierRequest(@NotBlank @Size(max=32) String code, @NotBlank @Size(max=120) String name, @Size(max=150) String contact) {}

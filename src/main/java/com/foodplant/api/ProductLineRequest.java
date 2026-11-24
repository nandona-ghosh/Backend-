package com.foodplant.api;
import jakarta.validation.constraints.*;
public record ProductLineRequest(@NotBlank @Size(max=32) String code, @NotBlank @Size(max=120) String name) {}

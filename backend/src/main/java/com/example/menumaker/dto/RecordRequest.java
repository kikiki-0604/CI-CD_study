package com.example.menumaker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record RecordRequest(
        @NotBlank(message = "dishName is required")
        String dishName,

        @NotBlank(message = "recipeUrl is required")
        @Pattern(regexp = "^https?://.+", message = "recipeUrl must be a valid URL")
        String recipeUrl,

        @NotBlank(message = "cookedOn is required")
        @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "cookedOn must be YYYY-MM-DD")
        String cookedOn
) {
}

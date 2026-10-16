package com.example.menumaker.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record SuggestionRequest(
        @NotEmpty(message = "ingredients is required")
        @Size(max = 10, message = "ingredients must be 10 or fewer")
        List<@Size(min = 1, max = 50, message = "each ingredient must be 1-50 characters") String> ingredients
) {
}

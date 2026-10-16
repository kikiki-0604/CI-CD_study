package com.example.menumaker.dto;

import java.util.List;

public record Candidate(
        String name,
        String description,
        int calories,
        int fat,
        int sugar,
        int carbs,
        List<RecipeLink> recipeLinks
) {
}

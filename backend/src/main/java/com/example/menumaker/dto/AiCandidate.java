package com.example.menumaker.dto;

/**
 * AIが返すJSONの1件分。項目が欠けた応答を検出できるよう、数値もIntegerで受ける
 * (欠けるとnullになる。検証はSuggestionServiceで行う)。
 */
public record AiCandidate(
        String name,
        String description,
        Integer calories,
        Integer fat,
        Integer sugar,
        Integer carbs
) {
}

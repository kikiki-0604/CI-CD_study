package com.example.menumaker.controller;

import com.example.menumaker.dto.SuggestionRequest;
import com.example.menumaker.dto.SuggestionResponse;
import com.example.menumaker.service.SuggestionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/suggestions")
public class SuggestionController {

    private final SuggestionService suggestionService;

    public SuggestionController(SuggestionService suggestionService) {
        this.suggestionService = suggestionService;
    }

    @PostMapping
    public SuggestionResponse suggest(@Valid @RequestBody SuggestionRequest request) {
        return suggestionService.suggest(request.ingredients());
    }
}

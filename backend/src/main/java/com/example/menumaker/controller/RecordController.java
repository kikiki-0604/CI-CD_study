package com.example.menumaker.controller;

import com.example.menumaker.dto.CreateRecordResponse;
import com.example.menumaker.dto.RecordRequest;
import com.example.menumaker.dto.RecordResponse;
import com.example.menumaker.service.RecordService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/records")
public class RecordController {

    private final RecordService recordService;

    public RecordController(RecordService recordService) {
        this.recordService = recordService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateRecordResponse create(@Valid @RequestBody RecordRequest request) {
        return recordService.create(request);
    }

    @GetMapping
    public List<RecordResponse> list() {
        return recordService.list();
    }
}

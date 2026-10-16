package com.example.menumaker.service;

import com.example.menumaker.dto.CreateRecordResponse;
import com.example.menumaker.dto.RecordRequest;
import com.example.menumaker.dto.RecordResponse;
import com.example.menumaker.entity.CookingRecord;
import com.example.menumaker.repository.CookingRecordRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class RecordService {

    private final CookingRecordRepository repository;

    public RecordService(CookingRecordRepository repository) {
        this.repository = repository;
    }

    public CreateRecordResponse create(RecordRequest request) {
        CookingRecord record = new CookingRecord(
                request.dishName(),
                request.recipeUrl(),
                LocalDate.parse(request.cookedOn())
        );
        CookingRecord saved = repository.save(record);
        return new CreateRecordResponse(saved.getId());
    }

    public List<RecordResponse> list() {
        return repository.findAllByOrderByCookedOnDesc().stream()
                .map(record -> new RecordResponse(
                        record.getId(),
                        record.getDishName(),
                        record.getRecipeUrl(),
                        record.getCookedOn().toString()))
                .toList();
    }
}

package com.example.menumaker.repository;

import com.example.menumaker.entity.CookingRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CookingRecordRepository extends JpaRepository<CookingRecord, Long> {

    List<CookingRecord> findAllByOrderByCookedOnDesc();
}

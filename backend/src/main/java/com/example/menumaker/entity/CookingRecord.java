package com.example.menumaker.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "cooking_records")
public class CookingRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "dish_name", nullable = false)
    private String dishName;

    @Column(name = "recipe_url", nullable = false, length = 1024)
    private String recipeUrl;

    @Column(name = "cooked_on", nullable = false)
    private LocalDate cookedOn;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    /** JPAが使うための引数なしコンストラクタ。 */
    protected CookingRecord() {
    }

    public CookingRecord(String dishName, String recipeUrl, LocalDate cookedOn) {
        this.dishName = dishName;
        this.recipeUrl = recipeUrl;
        this.cookedOn = cookedOn;
    }

    public Long getId() {
        return id;
    }

    public String getDishName() {
        return dishName;
    }

    public String getRecipeUrl() {
        return recipeUrl;
    }

    public LocalDate getCookedOn() {
        return cookedOn;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}

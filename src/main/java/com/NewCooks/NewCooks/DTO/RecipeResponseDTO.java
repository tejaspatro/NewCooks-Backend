package com.NewCooks.NewCooks.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecipeResponseDTO {
    private Long recipeId;
    private String title;
    private String description;
    private List<String> Ingredients;
    private List<String> Utensils;
    private String NutritionInfo;
    private Chef_User_DTO chef;
    private List<String> instructions;

    // New fields
    private String thumbnail;
    private List<String> images;
}

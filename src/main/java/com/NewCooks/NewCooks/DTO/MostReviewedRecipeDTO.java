package com.NewCooks.NewCooks.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MostReviewedRecipeDTO {
    private Long recipeId;
    private String title;
    private String thumbnail;
    private Long totalReviews;
}
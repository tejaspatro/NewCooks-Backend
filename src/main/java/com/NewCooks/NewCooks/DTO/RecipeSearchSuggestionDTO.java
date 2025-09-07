package com.NewCooks.NewCooks.DTO;

import com.NewCooks.NewCooks.Entity.Recipe;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecipeSearchSuggestionDTO {
    private Long recipeId;
    private String title;
    private String shortDescription;

    public static RecipeSearchSuggestionDTO fromEntity(Recipe recipe) {
        return new RecipeSearchSuggestionDTO(
                recipe.getRecipeId(),
                recipe.getTitle(),
                recipe.getDescription() != null && recipe.getDescription().length() > 80
                        ? recipe.getDescription().substring(0, 80) + "..."
                        : recipe.getDescription()
        );
    }
}

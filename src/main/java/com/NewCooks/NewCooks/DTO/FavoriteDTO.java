package com.NewCooks.NewCooks.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FavoriteDTO {
    private Long recipeId;
    private boolean isFavorite;
}

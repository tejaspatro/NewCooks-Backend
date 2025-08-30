package com.NewCooks.NewCooks.DTO;

import com.NewCooks.NewCooks.Entity.RatingEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RatingResponseDTO {
    private Long id;
    private Chef_User_DTO user;
    private Long recipeId;
    private int ratingValue;

    // This is a helper method to map the entity to the DTO
    public static RatingResponseDTO fromEntity(RatingEntity ratingEntity) {
        if (ratingEntity == null) {
            return null;
        }
        Chef_User_DTO Chef_User_DTO = new Chef_User_DTO(
                ratingEntity.getUser().getUserId(),
                ratingEntity.getUser().getName(),
                ratingEntity.getUser().getEmail()
        );
        return new RatingResponseDTO(
                ratingEntity.getId(),
                Chef_User_DTO,
                ratingEntity.getRecipe().getRecipeId(),
                ratingEntity.getRatingValue()
        );
    }
}

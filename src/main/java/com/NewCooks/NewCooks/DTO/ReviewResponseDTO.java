package com.NewCooks.NewCooks.DTO;

import com.NewCooks.NewCooks.Entity.ReviewEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponseDTO {
    private Long id;
    private Chef_User_DTO user;
    private Long recipeId;
    private String reviewText;

    // Helper method to map from entity to DTO
    public static ReviewResponseDTO fromEntity(ReviewEntity reviewEntity) {
        if (reviewEntity == null) {
            return null;
        }
        Chef_User_DTO Chef_User_DTO = new Chef_User_DTO(
                reviewEntity.getUser().getUserId(),
                reviewEntity.getUser().getName(),
                reviewEntity.getUser().getEmail()
        );
        return new ReviewResponseDTO(
                reviewEntity.getId(),
                Chef_User_DTO,
                reviewEntity.getRecipe().getRecipeId(),
                reviewEntity.getReviewText()
        );
    }
}

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
    private String userName;
    private Long recipeId;
    private String reviewText;
    private String profilePicture;
    private String aboutMe;

    // Helper method to map from entity to DTO
    public static ReviewResponseDTO fromEntity(ReviewEntity reviewEntity) {
        if (reviewEntity == null) {
            return null;
        }
        return new ReviewResponseDTO(
                reviewEntity.getId(),
                reviewEntity.getUser().getName(),
                reviewEntity.getRecipe().getRecipeId(),
                reviewEntity.getReviewText(),
                reviewEntity.getUser().getProfilePicture(),
                reviewEntity.getUser().getAboutMe()
        );
    }
}

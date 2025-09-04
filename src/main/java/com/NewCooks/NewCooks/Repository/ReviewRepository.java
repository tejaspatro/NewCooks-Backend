package com.NewCooks.NewCooks.Repository;

import com.NewCooks.NewCooks.Entity.Recipe;
import com.NewCooks.NewCooks.Entity.ReviewEntity;
import com.NewCooks.NewCooks.Entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<ReviewEntity, Long> {
    Optional<ReviewEntity> findByUserAndRecipe(User user, Recipe recipe);
    List<ReviewEntity> findByRecipe(Recipe recipe);
    List<ReviewEntity> findByRecipe_RecipeId(Long recipeId);
    List<ReviewEntity> findByRecipe_RecipeIdIn(List<Long> recipeIds);

    @Query("SELECT r.recipeId as recipeId, r.title as title, r.thumbnail as thumbnail, " +
            "COUNT(re.id) as totalReviews " +
            "FROM Recipe r JOIN r.reviews re " +
            "GROUP BY r.recipeId, r.title, r.thumbnail " +
            "ORDER BY COUNT(re.id) DESC")
    List<Object[]> findMostReviewedRecipes(Pageable pageable);
}
package com.NewCooks.NewCooks.Repository;

import com.NewCooks.NewCooks.Entity.Recipe;
import com.NewCooks.NewCooks.Entity.ReviewEntity;
import com.NewCooks.NewCooks.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<ReviewEntity, Long> {
    Optional<ReviewEntity> findByUserAndRecipe(User user, Recipe recipe);
    List<ReviewEntity> findByRecipe(Recipe recipe);
    List<ReviewEntity> findByRecipe_RecipeId(Long recipeId);

}
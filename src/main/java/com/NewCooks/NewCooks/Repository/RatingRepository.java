package com.NewCooks.NewCooks.Repository;

import com.NewCooks.NewCooks.Entity.RatingEntity;
import com.NewCooks.NewCooks.Entity.Recipe;
import com.NewCooks.NewCooks.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RatingRepository extends JpaRepository<RatingEntity, Long> {
    Optional<RatingEntity> findByUserAndRecipe(User user, Recipe recipe);
    List<RatingEntity> findByRecipe(Recipe recipe);

    @Query("SELECT AVG(r.ratingValue) FROM RatingEntity r WHERE r.recipe = :recipe")
    Double findAverageRatingByRecipe(@Param("recipe") Recipe recipe);

    @Query("SELECT r.ratingValue, COUNT(r) FROM RatingEntity r WHERE r.recipe = :recipe GROUP BY r.ratingValue")
    List<Object[]> countRatingsByStars(@Param("recipe") Recipe recipe);
}

package com.NewCooks.NewCooks.Repository;

import com.NewCooks.NewCooks.Entity.Recipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecipeRepository extends JpaRepository<Recipe, Long> {
    List<Recipe> findByChefId(Long chefId);
    boolean existsByTitleIgnoreCaseAndChefId(String title, Long chefId);
}

package com.NewCooks.NewCooks.Repository;

import com.NewCooks.NewCooks.Entity.Recipe;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecipeRepository extends JpaRepository<Recipe, Long> {
    List<Recipe> findByChefId(Long chefId);
    boolean existsByTitleIgnoreCaseAndChefId(String title, Long chefId);

    @Query("SELECT COUNT(r) FROM Recipe r WHERE r.chef.id = :chefId")
    int countByChefId(@Param("chefId") Long chefId);

    @Query("SELECT r FROM Recipe r WHERE LOWER(r.title) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Recipe> searchRecipesByKeyword(@Param("keyword") String keyword);

}

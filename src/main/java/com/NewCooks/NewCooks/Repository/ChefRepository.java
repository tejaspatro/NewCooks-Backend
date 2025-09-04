package com.NewCooks.NewCooks.Repository;

import com.NewCooks.NewCooks.Entity.Chef;
import com.NewCooks.NewCooks.Entity.Recipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChefRepository extends JpaRepository<Chef, Long> {
    Optional<Chef> findByEmail(String email);
    boolean existsByEmail(String email);
    Optional<Chef> findByActivationToken(String token);

    @Query("SELECT r FROM Recipe r WHERE r.chef.id = :chefId " +
            "AND (LOWER(r.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(r.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Recipe> searchRecipesByChefAndKeyword(@Param("chefId") Long chefId,
                                               @Param("keyword") String keyword);


}

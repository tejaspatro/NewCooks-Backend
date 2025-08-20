package com.NewCooks.NewCooks.Repository;

import com.NewCooks.NewCooks.Entity.Chef;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChefRepository extends JpaRepository<Chef, Long> {
    Optional<Chef> findByEmail(String email);
    boolean existsByEmail(String email);
    Optional<Chef> findByActivationToken(String token);

}

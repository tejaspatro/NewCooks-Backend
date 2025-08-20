package com.NewCooks.NewCooks.Controller;

import com.NewCooks.NewCooks.DTO.RecipeDTO;
import com.NewCooks.NewCooks.DTO.RecipeResponseDTO;
import com.NewCooks.NewCooks.Entity.Recipe;
import com.NewCooks.NewCooks.Service.ChefService;
import com.NewCooks.NewCooks.Service.RecipeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/chef")
public class ChefController
{
    private final RecipeService recipeService;
    private final ChefService chefService;

    public ChefController(RecipeService recipeService, ChefService chefService) {
        this.recipeService = recipeService;
        this.chefService = chefService;
    }

    // Helper method to check if logged-in user matches path chefId
    private boolean isAuthorized(Long chefId) {
        String loggedInEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        return chefService.findByEmail(loggedInEmail)
                .map(loggedInChef -> loggedInChef.getId().equals(chefId))
                .orElse(false);
    }


    @PostMapping("/{chefId}/recipes")
    public ResponseEntity<?> addRecipe(@PathVariable Long chefId, @RequestBody RecipeDTO dto) {
        if (!isAuthorized(chefId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You are not authorized to add recipes for this chef.");
        }
        Recipe created = recipeService.addRecipe(chefId, dto);
        RecipeResponseDTO responseDTO = recipeService.toRecipeResponseDTO(created);
        return ResponseEntity.ok(responseDTO);
    }

    @PutMapping("/{chefId}/recipes/{recipeId}")
    public ResponseEntity<?> updateRecipe(@PathVariable Long chefId,
                                          @PathVariable Long recipeId,
                                          @RequestBody RecipeDTO dto) {
        if (!isAuthorized(chefId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You are not authorized to update recipes for this chef.");
        }
        Recipe updated = recipeService.updateRecipe(chefId, recipeId, dto)
                .orElseThrow(() -> new RuntimeException("Cannot update recipe"));
        RecipeResponseDTO responseDTO = recipeService.toRecipeResponseDTO(updated);
        return ResponseEntity.ok(responseDTO);

    }

    @DeleteMapping("/{chefId}/recipes/{recipeId}")
    public ResponseEntity<?> deleteRecipe(@PathVariable Long chefId, @PathVariable Long recipeId) {
        if (!isAuthorized(chefId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You are not authorized to delete recipes for this chef.");
        }
        recipeService.deleteRecipe(chefId, recipeId);
        return ResponseEntity.ok("Recipe deleted");
    }

    @GetMapping("/{chefId}/recipes")
    public ResponseEntity<?> getMyRecipes(@PathVariable Long chefId) {
        if (!isAuthorized(chefId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You are not authorized to view recipes for this chef.");
        }
        List<Recipe> recipes = recipeService.getRecipesByChef(chefId);
        List<RecipeResponseDTO> dtoList = recipes.stream()
                .map(recipeService::toRecipeResponseDTO)
                .toList();

        return ResponseEntity.ok(dtoList);
    }

    @GetMapping("/{chefId}/recipes/{recipeId}")
    public ResponseEntity<?> getMyRecipeById(@PathVariable Long chefId, @PathVariable Long recipeId) {
        if (!isAuthorized(chefId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("You are not authorized to view recipes for this chef.");
        }
        return recipeService.getRecipeById(recipeId)
                .map(recipeService::toRecipeResponseDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

}

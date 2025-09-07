package com.NewCooks.NewCooks.Controller;

import com.NewCooks.NewCooks.DTO.ChefAnalyticsDTO;
import com.NewCooks.NewCooks.DTO.MostReviewedRecipeDTO;
import com.NewCooks.NewCooks.DTO.RecipeResponseDTO;
import com.NewCooks.NewCooks.Entity.Recipe;
import com.NewCooks.NewCooks.Service.RecipeService;
import com.NewCooks.NewCooks.Service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/recipes")
@CrossOrigin(origins = "${newcooks.frontend.url}")
public class RecipeController {

    private final RecipeService recipeService;
    private final UserService userService;

    public RecipeController(RecipeService recipeService, UserService userService) {
        this.recipeService = recipeService;
        this.userService = userService;
    }

//    @GetMapping
//    public ResponseEntity<Page<RecipeResponseDTO>> getAllRecipes(
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "16") int size
//    ) {
//        Page<Recipe> recipesPage = recipeService.getAllRecipes(page, size);
//        Page<RecipeResponseDTO> dtoList = recipesPage.map(recipeService::toRecipeResponseDTO);
//        return ResponseEntity.ok(dtoList);
//    }

//    @GetMapping("/{id}")
//    public ResponseEntity<?> getRecipeById(@PathVariable Long id) {
//        return recipeService.getRecipeById(id)
//                .map(recipeService::toRecipeResponseDTO)
//                .map(ResponseEntity::ok)
//                .orElse(ResponseEntity.notFound().build());
//    }




    @GetMapping("/{recipeId}/average-rating")
    public ResponseEntity<Double> getAverageRating(@PathVariable Long recipeId) {
        return ResponseEntity.ok(recipeService.getAverageRating(recipeId));
    }

    @GetMapping("/rating/{recipeId}")
    public ResponseEntity<?> getRatingForRecipe(@PathVariable Long recipeId) {
        try {
            return ResponseEntity.ok(recipeService.getRatingStats(recipeId));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @GetMapping("/most-reviewed/{limit}")
    public ResponseEntity<List<MostReviewedRecipeDTO>> getChefMostReviewed(@PathVariable int limit) {
        List<MostReviewedRecipeDTO> mostReviewed = recipeService.getMostReviewedRecipes(limit);
        return ResponseEntity.ok(mostReviewed);
    }

}

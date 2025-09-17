package com.NewCooks.NewCooks.Controller;

import com.NewCooks.NewCooks.DTO.MostReviewedRecipeDTO;
import com.NewCooks.NewCooks.Service.RecipeService;
import com.NewCooks.NewCooks.Service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

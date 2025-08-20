package com.NewCooks.NewCooks.Controller;

import com.NewCooks.NewCooks.DTO.RecipeResponseDTO;
import com.NewCooks.NewCooks.Entity.Recipe;
import com.NewCooks.NewCooks.Service.RecipeService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/recipes")
@CrossOrigin(origins = "http://localhost:5173")
public class RecipeController {

    private final RecipeService recipeService;

    public RecipeController(RecipeService recipeService) {
        this.recipeService = recipeService;
    }

    @GetMapping
    public ResponseEntity<Page<RecipeResponseDTO>> getAllRecipes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "16") int size
    ) {
        Page<Recipe> recipesPage = recipeService.getAllRecipes(page, size);
        Page<RecipeResponseDTO> dtoList = recipesPage.map(recipeService::toRecipeResponseDTO);
        return ResponseEntity.ok(dtoList);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getRecipeById(@PathVariable Long id) {
        return recipeService.getRecipeById(id)
                .map(recipeService::toRecipeResponseDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

}

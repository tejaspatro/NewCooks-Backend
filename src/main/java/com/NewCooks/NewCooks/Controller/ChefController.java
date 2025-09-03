package com.NewCooks.NewCooks.Controller;

import com.NewCooks.NewCooks.DTO.*;
import com.NewCooks.NewCooks.Entity.Chef;
import com.NewCooks.NewCooks.Entity.Recipe;
import com.NewCooks.NewCooks.Repository.ChefRepository;
import com.NewCooks.NewCooks.Service.ChefService;
import com.NewCooks.NewCooks.Service.RecipeService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/chef")
@AllArgsConstructor
public class ChefController
{
    private final RecipeService recipeService;
    private final ChefService chefService;


    // Helper method to check if logged-in user matches path chefId
    private boolean isAuthorized(Long chefId) {
        String loggedInEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        return chefService.findByEmail(loggedInEmail)
                .map(loggedInChef -> loggedInChef.getId().equals(chefId))
                .orElse(false);
    }

    @PostMapping("/recipes")
    public ResponseEntity<?> addRecipe(@RequestBody RecipeDTO dto, Principal principal){

        String loggedInUsername = principal.getName();
        Long chefId = chefService.findByEmail(loggedInUsername)
                .orElseThrow(() -> new RuntimeException("Chef not found"))
                .getId();

        if (!isAuthorized(chefId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You are not authorized to add recipes for this chef.");
        }
        Recipe created = recipeService.addRecipe(chefId, dto);
        RecipeResponseDTO responseDTO = recipeService.toRecipeResponseDTO(created);
        return ResponseEntity.ok(responseDTO);
    }

    @PutMapping("recipes/{recipeId}")
    public ResponseEntity<?> updateRecipe(@PathVariable Long recipeId,
                                          @RequestBody RecipeDTO dto,
                                          Principal principal) {
        String loggedInUsername = principal.getName();
        Long chefId = chefService.findByEmail(loggedInUsername)
                .orElseThrow(() -> new RuntimeException("Chef not found"))
                .getId();

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

    @GetMapping("/recipes")
    public ResponseEntity<?> getMyRecipes(Principal principal) {

        String loggedInUsername = principal.getName();
        Long chefId = chefService.findByEmail(loggedInUsername)
                .orElseThrow(() -> new RuntimeException("Chef not found"))
                .getId();

        if (!isAuthorized(chefId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You are not authorized to view recipes for this chef.");
        }
        List<Recipe> recipes = recipeService.getRecipesByChef(chefId);
        List<RecipeResponseDTO> dtoList = recipes.stream()
                .map(recipeService::toRecipeResponseDTO)
                .toList();

        return ResponseEntity.ok(dtoList);
    }

    @GetMapping("/recipes/{recipeId}")
    public ResponseEntity<?> getMyRecipeById(Principal principal, @PathVariable Long recipeId) {
        String loggedInUsername = principal.getName();
        Long chefId = chefService.findByEmail(loggedInUsername)
                .orElseThrow(() -> new RuntimeException("Chef not found"))
                .getId();
        if (!isAuthorized(chefId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("You are not authorized to view recipes for this chef.");
        }
        return recipeService.getRecipeById(recipeId)
                .map(recipeService::toRecipeResponseDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{chefId}/recipes/{recipeId}/images/delete")
    public ResponseEntity<?> deleteRecipeImage(
            @PathVariable Long chefId,
            @PathVariable Long recipeId,
            @RequestBody Map<String, String> body
    ) {
        if (!isAuthorized(chefId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You are not authorized to delete images for this chef.");
        }

        String url = body.get("url");
        if (url == null || url.isEmpty()) {
            return ResponseEntity.badRequest().body("Image URL is required");
        }

        recipeService.removeImage(recipeId, url);
        return ResponseEntity.ok(Map.of("message", "Image deleted successfully"));
    }

    @GetMapping("/chefprofile")
    public ResponseEntity<ChefProfileDTO> getLoggedInChefProfile(Principal principal) {
        if (principal == null) {
            throw new RuntimeException("Unauthorized: Principal is null");
        }
        String email = principal.getName();

        Chef chef = chefService.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Chef not found"));


        ChefProfileDTO dto = new ChefProfileDTO(
                chef.getId(),
                chef.getName(),
                chef.getEmail(),
                chef.getExpertise(),
                chef.getExperience(),
                chef.getBio(),
                chef.getProfilePicture()
        );
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/chefprofile")
    public ResponseEntity<ChefProfileDTO> updateChefProfile(@RequestBody ChefProfileDTO dto, Principal principal) {
        String email = principal.getName(); // get logged-in chef's email

        Chef updatedChef = chefService.updateChefProfile(
                email,
                dto.getName(),
                dto.getExpertise(),
                dto.getExperience(),
                dto.getBio(),
                dto.getProfilePicture()
        );

        ChefProfileDTO responseDTO = new ChefProfileDTO(
                updatedChef.getId(),
                updatedChef.getName(),
                updatedChef.getEmail(),
                updatedChef.getExpertise(),
                updatedChef.getExperience(),
                updatedChef.getBio(),
                updatedChef.getProfilePicture()
        );

        return ResponseEntity.ok(responseDTO);
    }

    @GetMapping("/recipes/{recipeId}/reviews")
    public ResponseEntity<?> getReviewsForRecipeByChef(@PathVariable Long recipeId, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body("Unauthorized: Principal is null");
        }

        String email = principal.getName();

        try {
            // Optional: Check if this recipe belongs to the logged-in chef
            Recipe recipe = recipeService.getRecipeById(recipeId)
                    .orElseThrow(() -> new RuntimeException("Recipe not found"));

            if (!recipe.getChef().getEmail().equals(email)) {
                return ResponseEntity.status(403).body("Forbidden: Not your recipe");
            }

            // Fetch reviews
            return ResponseEntity.ok(recipeService.getReviewsForRecipe(recipeId));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PreAuthorize("hasRole('CHEF')")
    @GetMapping("/recipes/search")
    public ResponseEntity<List<ChefRecipeSearchSuggestionDTO>> searchChefRecipes(
            @RequestParam String keyword,
            Principal principal) {

        // Get logged-in username (email or username depending on your setup)
        String loggedInUsername = principal.getName();

        // Look up chefId securely from DB
        Long chefId = chefService.findByEmail(loggedInUsername)
                .orElseThrow(() -> new RuntimeException("Chef not found"))
                .getId();

        List<ChefRecipeSearchSuggestionDTO> results = chefService.searchChefRecipes(chefId, keyword);
        return ResponseEntity.ok(results);
    }


    @GetMapping("/test")
        public String test()
        {
            return "test successful";
        }

}

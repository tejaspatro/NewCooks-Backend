package com.NewCooks.NewCooks.Controller;

import com.NewCooks.NewCooks.DTO.*;
import com.NewCooks.NewCooks.Entity.Chef;
import com.NewCooks.NewCooks.Entity.Recipe;
import com.NewCooks.NewCooks.Repository.ChefRepository;
import com.NewCooks.NewCooks.Service.ChefService;
import com.NewCooks.NewCooks.Service.RecipeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

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
    public ResponseEntity<?> addRecipe(@PathVariable Long chefId, @RequestBody RecipeDTO dto){
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


    @GetMapping("/test")
        public String test()
        {
            return "test successful";
        }

}

package com.NewCooks.NewCooks.Controller;

import com.NewCooks.NewCooks.DTO.*;
import com.NewCooks.NewCooks.Entity.Chef;
import com.NewCooks.NewCooks.Entity.Recipe;
import com.NewCooks.NewCooks.Repository.ChefRepository;
import com.NewCooks.NewCooks.Service.ChefService;
import com.NewCooks.NewCooks.Service.CloudinaryService;
import com.NewCooks.NewCooks.Service.RecipeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/chef")
@AllArgsConstructor
public class ChefController
{
    private final RecipeService recipeService;
    private final CloudinaryService cloudinaryService;
    private final ChefService chefService;
    private final ObjectMapper objectMapper;


    // Helper method to check if logged-in user matches path chefId
    private boolean isAuthorized(Long chefId) {
        String loggedInEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        return chefService.findByEmail(loggedInEmail)
                .map(loggedInChef -> loggedInChef.getId().equals(chefId))
                .orElse(false);
    }

    @PostMapping(value = "/recipes", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    public ResponseEntity<?> addRecipe(@RequestPart("recipe") String recipeDtoString,
                                       @RequestPart(value = "thumbnailFile", required = false) MultipartFile thumbnailFile,
                                       @RequestPart(value = "imageFiles", required = false) List<MultipartFile> imageFiles,
                                       Principal principal) {
        try {
            RecipeDTO dto = objectMapper.readValue(recipeDtoString, RecipeDTO.class);
            String loggedInUsername = principal.getName();
            Long chefId = chefService.findByEmail(loggedInUsername)
                    .orElseThrow(() -> new RuntimeException("Chef not found"))
                    .getId();

            Recipe created = recipeService.addRecipe(chefId, dto, thumbnailFile, imageFiles);
            RecipeResponseDTO responseDTO = recipeService.toRecipeResponseDTO(created);
            return ResponseEntity.ok(responseDTO);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error processing request: " + e.getMessage());
        }
    }

    @PutMapping(value = "/recipes/{recipeId}", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    public ResponseEntity<?> updateRecipe(@PathVariable Long recipeId,
                                          @RequestPart("recipe") String recipeDtoString, // Receive DTO as a JSON string
                                          @RequestPart(value = "newThumbnailFile", required = false) MultipartFile newThumbnailFile,
                                          @RequestPart(value = "newImageFiles", required = false) List<MultipartFile> newImageFiles,
                                          Principal principal) {
        try {
            // Deserialize the JSON string to our DTO object
            RecipeDTO dto = objectMapper.readValue(recipeDtoString, RecipeDTO.class);

            String loggedInUsername = principal.getName();
            Long chefId = chefService.findByEmail(loggedInUsername)
                    .orElseThrow(() -> new RuntimeException("Chef not found"))
                    .getId();

            // The isAuthorized check is no longer needed here as we get the chefId from the principal

            Recipe updated = recipeService.updateRecipe(chefId, recipeId, dto, newThumbnailFile, newImageFiles)
                    .orElseThrow(() -> new RuntimeException("Cannot update recipe"));

            RecipeResponseDTO responseDTO = recipeService.toRecipeResponseDTO(updated);
            return ResponseEntity.ok(responseDTO);

        } catch (Exception e) {
            // Handle JSON parsing errors or other issues
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error processing request: " + e.getMessage());
        }
    }

    @DeleteMapping("/recipes/{recipeId}")
    public ResponseEntity<?> deleteRecipe(Principal principal, @PathVariable Long recipeId) {

        String loggedInUsername = principal.getName();
        Long chefId = chefService.findByEmail(loggedInUsername)
                .orElseThrow(() -> new RuntimeException("Chef not found"))
                .getId();

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

        cloudinaryService.removeImage(recipeId, url);
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

    @PutMapping(value = "/chefprofile", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    public ResponseEntity<?> updateChefProfile(
            @RequestPart("profile") String profileDtoString,
            @RequestPart(value = "profilePictureFile", required = false) MultipartFile profilePictureFile,
            Principal principal) {

        try {
            ChefProfileDTO dto = objectMapper.readValue(profileDtoString, ChefProfileDTO.class);
            String email = principal.getName();

            Chef updatedChef = chefService.updateChefProfile(email, dto, profilePictureFile);

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

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error processing profile update: " + e.getMessage());
        }
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

    @GetMapping("/analytics")
    public ChefAnalyticsDTO getChefAnalytics(Principal principal) {

        String loggedInUsername = principal.getName();
        Long chefId = chefService.findByEmail(loggedInUsername)
                .orElseThrow(() -> new RuntimeException("Chef not found"))
                .getId();
        return recipeService.getChefAnalytics(chefId);
    }

    @GetMapping("/test")
        public String test()
        {
            return "test successful";
        }

}

package com.NewCooks.NewCooks.Controller;

import com.NewCooks.NewCooks.DTO.*;
import com.NewCooks.NewCooks.Entity.RatingEntity;
import com.NewCooks.NewCooks.Entity.Recipe;
import com.NewCooks.NewCooks.Entity.ReviewEntity;
import com.NewCooks.NewCooks.Entity.User;
import com.NewCooks.NewCooks.Repository.RatingRepository;
import com.NewCooks.NewCooks.Repository.RecipeRepository;
import com.NewCooks.NewCooks.Repository.ReviewRepository;
import com.NewCooks.NewCooks.Repository.UserRepository;
import com.NewCooks.NewCooks.Service.RecipeService;
import com.NewCooks.NewCooks.Service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/user")
@CrossOrigin(origins = "${newcooks.frontend.url}")
@AllArgsConstructor
public class UserController {

    private final RecipeService recipeService;
    private final UserRepository userRepository;
    private final UserService userService;
    private final RatingRepository ratingRepository;
    private final RecipeRepository recipeRepository;
    private final ReviewRepository reviewRepository;

    private boolean isAuthorized(Long userId) {
        String loggedInEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        return userService.findByEmail(loggedInEmail)
                .map(loggedInChef -> loggedInChef.getUserId().equals(userId))
                .orElse(false);
    }

    @GetMapping("/recipes")
    public ResponseEntity<Page<RecipeResponseDTO>> viewAllRecipes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        Page<Recipe> recipesPage = recipeService.getAllRecipes(page, size);
        Page<RecipeResponseDTO> dtoList = recipesPage.map(recipeService::toRecipeResponseDTO);
        return ResponseEntity.ok(dtoList);
    }

    @GetMapping("/recipes/{recipeId}")
    public ResponseEntity<?> viewRecipe(@PathVariable Long recipeId) {
        return recipeService.getRecipeById(recipeId)
                .map(recipeService::toRecipeResponseDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }


    @GetMapping("/activate")
    public ResponseEntity<?> activateUser(@RequestParam String token) {
        Optional<User> userOpt = userRepository.findByActivationToken(token);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid activation token");
        }
        User user = userOpt.get();
        user.setActive(true);
        user.setActivationToken(null); // clear token after activation
        userRepository.save(user);
        return ResponseEntity.ok("Account activated successfully");
    }

    @PostMapping("/ratings/{recipeId}")
    public ResponseEntity<?> addOrUpdateRating(
            @PathVariable Long recipeId,
            @RequestBody RatingDTO ratingDTO
    ) {
        String loggedInEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        Optional<User> userOpt = userService.findByEmail(loggedInEmail);

        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("User not authorized or not found.");
        }

        Long userId = userOpt.get().getUserId();

        try {
            return ResponseEntity.ok(recipeService.addOrUpdateRating(recipeId, userId, ratingDTO.getStars()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/recipes/user-rating/{recipeId}")
    public ResponseEntity<RatingDTO> getUserRatingForRecipe(
            @PathVariable Long recipeId,
            Principal principal) {

        String email = principal.getName();
        User user = userService.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new RuntimeException("Recipe not found"));

        Optional<RatingEntity> rating = ratingRepository.findByUserAndRecipe(user, recipe);

        int stars = rating.map(RatingEntity::getRatingValue).orElse(0);
        RatingDTO dto = new RatingDTO(stars);

        return ResponseEntity.ok(dto);
    }


    @DeleteMapping("/ratings/{recipeId}")
    public ResponseEntity<?> deleteRating(
            @PathVariable Long recipeId
    ) {
        String loggedInEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        Optional<User> userOpt = userService.findByEmail(loggedInEmail);

        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("User not authorized or not found.");
        }
        Long userId = userOpt.get().getUserId();

        try {
            recipeService.deleteRating(userId, recipeId);
            return ResponseEntity.ok("Review deleted successfully.");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/reviews/{recipeId}")
    public ResponseEntity<?> addOrUpdateReview(
            @PathVariable Long recipeId,
            @RequestBody ReviewDTO reviewDTO
    ) {
        String loggedInEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        Optional<User> userOpt = userService.findByEmail(loggedInEmail);

        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("User not authorized or not found.");
        }
        Long userId = userOpt.get().getUserId();

        try {
            return ResponseEntity.ok(recipeService.addOrUpdateReview(recipeId, userId, reviewDTO.getReviewText()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/reviews/{recipeId}")
    public ResponseEntity<?> getReviewsForRecipe(@PathVariable Long recipeId) {
        try {
            return ResponseEntity.ok(recipeService.getReviewsForRecipe(recipeId));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/recipes/my-review/{recipeId}")
    public ResponseEntity<ReviewDTO> getUserReviewForRecipe(
            @PathVariable Long recipeId,
            Principal principal) {

        String email = principal.getName();
        User user = userService.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new RuntimeException("Recipe not found"));

        Optional<ReviewEntity> review = reviewRepository.findByUserAndRecipe(user, recipe);

        if (review.isPresent()) {
            ReviewEntity r = review.get();
            ReviewDTO dto = new ReviewDTO(r.getId(), r.getReviewText());
            return ResponseEntity.ok(dto);
        } else {
            // Return DTO with null id and empty text if no review
            return ResponseEntity.ok(new ReviewDTO(null, ""));
        }
    }




    @DeleteMapping("/reviews/{reviewId}")
    public ResponseEntity<?> deleteReview(
            @PathVariable Long reviewId,
            Principal principal
    ) {

        String email = principal.getName();
        Optional<User> userOpt = userService.findByEmail(email);

        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("User not authorized or not found.");
        }
        Long userId = userOpt.get().getUserId();

        try {
            recipeService.deleteReview(userId, reviewId);
            return ResponseEntity.ok("Review deleted successfully.");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/userprofile")
    public ResponseEntity<UserProfileDTO> getLoggedInUserProfile(Principal principal) {
        if (principal == null) {
            throw new RuntimeException("Unauthorized: Principal is null");
        }

        String email = principal.getName();

        User user = userService.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        UserProfileDTO dto = new UserProfileDTO(
                user.getUserId(),
                user.getName(),
                user.getEmail(),
                user.getProfilePicture(),
                user.getAboutMe()
        );

        return ResponseEntity.ok(dto);
    }

    @PutMapping("/userprofile")
    public ResponseEntity<UserProfileDTO> updateUserProfile(@RequestBody UserProfileDTO dto, Principal principal) {
        String email = principal.getName(); // get logged-in user's email

        User updatedUser = userService.updateUserProfile(
                email,
                dto.getName(),
                dto.getProfilePicture(),
                dto.getAboutMe()
        );

        UserProfileDTO responseDTO = new UserProfileDTO(
                updatedUser.getUserId(),
                updatedUser.getName(),
                updatedUser.getEmail(),
                updatedUser.getProfilePicture(),
                updatedUser.getAboutMe()
        );

        return ResponseEntity.ok(responseDTO);
    }

    @GetMapping("/recipes/search")
    public ResponseEntity<List<RecipeSearchSuggestionDTO>> searchUserRecipes(
            @RequestParam String keyword) {

        List<RecipeSearchSuggestionDTO> results = userService.searchRecipes(keyword);
        return ResponseEntity.ok(results);
    }

    @PostMapping("/favourites/{recipeId}")
    public ResponseEntity<FavoriteDTO> toggleFavorite(
            @PathVariable Long recipeId,
            Principal principal) {
        String username = principal.getName();
        FavoriteDTO dto = userService.toggleFavorite(username, recipeId);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/favourites")
    public ResponseEntity<List<RecipeSearchSuggestionDTO>> getFavorites(Principal principal) {
        String username = principal.getName();
        List<RecipeSearchSuggestionDTO> favorites = userService.getUserFavorites(username);
        return ResponseEntity.ok(favorites);
    }

    @GetMapping("/analytics")
    public ResponseEntity<UserAnalyticsDTO> getUserAnalytics(Principal principal) {
        Long userId = userService.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getUserId();

        UserAnalyticsDTO analytics = recipeService.getUserAnalytics(userId);

        return ResponseEntity.ok(analytics);
    }


    @GetMapping("/test")
    public String test()
    {
        return "test successful";
    }

}


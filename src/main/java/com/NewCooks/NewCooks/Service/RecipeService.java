package com.NewCooks.NewCooks.Service;

import com.NewCooks.NewCooks.DTO.*;
import com.NewCooks.NewCooks.Entity.*;
import com.NewCooks.NewCooks.Repository.*;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecipeService {

    private final RecipeRepository recipeRepository;
    private final ChefRepository chefRepository;
    private final ReviewRepository reviewRepository;
    private final RatingRepository ratingRepository;
    private final UserRepository userRepository;
    private final Cloudinary cloudinary;


    public Recipe addRecipe(Long chefId, RecipeDTO dto){
        Chef chef = chefRepository.findById(chefId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chef not found"));

        boolean exists = recipeRepository.existsByTitleIgnoreCaseAndChefId(dto.getTitle(), chefId);
        if (exists) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Recipe title already exists for this chef");
        }

        Recipe r = new Recipe();
        r.setTitle(dto.getTitle());
        r.setDescription(dto.getDescription());
        r.setChef(chef);
        r.setIngredients(dto.getIngredients());
        r.setUtensils(dto.getUtensils());
        r.setNutritionInfo(dto.getNutritionInfo());
        r.setInstructions(dto.getInstructions());
        r.setThumbnail(dto.getThumbnail());
        r.setImages(dto.getImages());
        return recipeRepository.save(r);
    }

    public Optional<Recipe> updateRecipe(Long chefId, Long recipeId, RecipeDTO dto) {
        Recipe existing = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new RuntimeException("Recipe not found"));
        if (!existing.getChef().getId().equals(chefId)) {
            throw new RuntimeException("Cannot update another chef's recipe");
        }

        if (!existing.getTitle().equals(dto.getTitle())) {
            boolean exists = recipeRepository.existsByTitleIgnoreCaseAndChefId(dto.getTitle(), chefId);
            if (exists) {
                throw new RuntimeException("Recipe title already exists for this chef");
            }
        }

        existing.setTitle(dto.getTitle());
        existing.setDescription(dto.getDescription());
        existing.setIngredients(dto.getIngredients());
        existing.setUtensils(dto.getUtensils());
        existing.setNutritionInfo(dto.getNutritionInfo());
        existing.setInstructions(dto.getInstructions());
        existing.setThumbnail(dto.getThumbnail());
        existing.setImages(dto.getImages());

        return Optional.of(recipeRepository.save(existing));
    }

    public void deleteRecipe(Long chefId, Long recipeId) {
        Recipe existing = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new RuntimeException("Recipe not found"));
        if (!existing.getChef().getId().equals(chefId)) {
            throw new RuntimeException("Cannot delete another chef's recipe");
        }
        recipeRepository.delete(existing);
    }

    public Page<Recipe> getAllRecipes(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return recipeRepository.findAll(pageable);
    }

    public Optional<Recipe> getRecipeById(Long id) {
        return recipeRepository.findById(id);
    }

    public List<Recipe> getRecipesByChef(Long chefId) {
        return recipeRepository.findByChefId(chefId);
    }

    private RecipeDTO mapToDTO(Recipe recipe) {
        return new RecipeDTO(
                recipe.getRecipeId(),
                recipe.getTitle(),
                recipe.getDescription(),
                recipe.getIngredients(),
                recipe.getUtensils(),
                recipe.getNutritionInfo(),
                recipe.getInstructions(),
                recipe.getThumbnail(),
                recipe.getImages()
        );
    }

    public List<RecipeDTO> getRecipeDTOsByChef(Long chefId) {
        List<Recipe> recipes = recipeRepository.findByChefId(chefId);
        return recipes.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    public RecipeResponseDTO toRecipeResponseDTO(Recipe recipe)
    {
        Chef chef = recipe.getChef();
        Chef_User_DTO chefUserDTO = new Chef_User_DTO(chef.getId(), chef.getName(), chef.getEmail());
        return new RecipeResponseDTO(
                recipe.getRecipeId(),
                recipe.getTitle(),
                recipe.getDescription(),
                recipe.getIngredients(),
                recipe.getUtensils(),
                recipe.getNutritionInfo(),
                chefUserDTO,
                recipe.getInstructions(),
                recipe.getThumbnail(),
                recipe.getImages()
        );
    }

    // ========== Cloudinary Image Delete Logic ==========
    public void deleteImageFromCloud(String publicId) {
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Recipe removeImage(Long recipeId, String urlToRemove) {
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new RuntimeException("Recipe not found"));
        if (recipe.getImages().contains(urlToRemove)) {
            recipe.getImages().remove(urlToRemove);
            String publicId = extractPublicId(urlToRemove);
            deleteImageFromCloud(publicId);
            recipeRepository.save(recipe);
        }
        return recipe;
    }

    private String extractPublicId(String url) {
        // Cloudinary URL example: https://res.cloudinary.com/<cloud>/image/upload/v123456/<public_id>.jpg
        String[] parts = url.split("/");
        String filename = parts[parts.length - 1]; // "<public_id>.jpg"
        return filename.split("\\.")[0]; // "<public_id>"
    }

    // ===== Add or Update Review =====
    public ReviewEntity addOrUpdateReview(Long recipeId, Long userId, String comment) {
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new RuntimeException("Recipe not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        ReviewEntity review = reviewRepository.findByUserAndRecipe(user, recipe)
                .orElse(new ReviewEntity());

        review.setRecipe(recipe);
        review.setUser(user);
        review.setReviewText(comment);

        return reviewRepository.save(review);
    }

    // ===== Add or Update Rating =====
    @Transactional
    public RatingResponseDTO addOrUpdateRating(Long recipeId, Long userId, int stars) {
        if (stars < 0 || stars > 5) {
            throw new RuntimeException("Rating must be between 0 and 5");
        }

        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new RuntimeException("Recipe not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        RatingEntity rating = ratingRepository.findByUserAndRecipe(user, recipe)
                .orElse(new RatingEntity());

        rating.setRecipe(recipe);
        rating.setUser(user);
        rating.setRatingValue(stars);

        RatingEntity savedRating = ratingRepository.save(rating);

        return RatingResponseDTO.fromEntity(savedRating);
    }


    // ===== Get average rating =====
    public double getAverageRating(Long recipeId) {
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new RuntimeException("Recipe not found"));
        Double avg = ratingRepository.findAverageRatingByRecipe(recipe);
        return avg != null ? avg : 0.0;
    }

    // ===== Get all reviews for a recipe =====
    public List<ReviewEntity> getReviewsForRecipe(Long recipeId) {
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new RuntimeException("Recipe not found"));
        return reviewRepository.findByRecipe(recipe);
    }

    public void deleteReview(Long userId, Long recipeId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new RuntimeException("Recipe not found"));

        ReviewEntity review = reviewRepository.findByUserAndRecipe(user, recipe)
                .orElseThrow(() -> new RuntimeException("Review not found"));

        reviewRepository.delete(review);
    }

    public void deleteRating(Long userId, Long recipeId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new RuntimeException("Recipe not found"));

        RatingEntity rating = ratingRepository.findByUserAndRecipe(user, recipe)
                .orElseThrow(() -> new RuntimeException("Rating not found"));

        ratingRepository.delete(rating);
    }

}

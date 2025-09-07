package com.NewCooks.NewCooks.Service;

import com.NewCooks.NewCooks.DTO.*;
import com.NewCooks.NewCooks.Entity.*;
import com.NewCooks.NewCooks.Repository.*;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.*;
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
    final CloudinaryService cloudinaryService;


    public Recipe addRecipe(Long chefId, RecipeDTO dto, MultipartFile thumbnailFile, List<MultipartFile> imageFiles) {
        Chef chef = chefRepository.findById(chefId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chef not found"));

        if (recipeRepository.existsByTitleIgnoreCaseAndChefId(dto.getTitle(), chefId)) {
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

        // Upload and set thumbnail
        String thumbnailUrl = uploadFileToCloudinary(thumbnailFile);
        r.setThumbnail(thumbnailUrl);

        // Upload and set additional images
        if (imageFiles != null && !imageFiles.isEmpty()) {
            List<String> imageUrls = imageFiles.stream()
                    .map(this::uploadFileToCloudinary)
                    .collect(Collectors.toList());
            r.setImages(imageUrls);
        } else {
            r.setImages(new ArrayList<>());
        }

        return recipeRepository.save(r);
    }

    public Optional<Recipe> updateRecipe(Long chefId, Long recipeId, RecipeDTO dto, MultipartFile newThumbnailFile, List<MultipartFile> newImageFiles) {
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
        if (newThumbnailFile != null && !newThumbnailFile.isEmpty()) {
            // Delete old thumbnail from Cloudinary if it exists
            if (existing.getThumbnail() != null && !existing.getThumbnail().isEmpty()) {
                cloudinaryService.deleteImageFromCloud(cloudinaryService.extractPublicId(existing.getThumbnail()));
            }
            String newThumbnailUrl = uploadFileToCloudinary(newThumbnailFile);
            existing.setThumbnail(newThumbnailUrl);
        } else {
            // If the DTO's thumbnail is null/empty, it means it was removed on the frontend
            if (dto.getThumbnail() == null || dto.getThumbnail().isEmpty()) {
                if (existing.getThumbnail() != null && !existing.getThumbnail().isEmpty()) {
                    cloudinaryService.deleteImageFromCloud(cloudinaryService.extractPublicId(existing.getThumbnail()));
                }
                existing.setThumbnail(null);
            }
        }

        // Handle additional images update
        List<String> finalImageUrls = new ArrayList<>(dto.getImages() != null ? dto.getImages() : List.of());
        if (newImageFiles != null && !newImageFiles.isEmpty()) {
            for (MultipartFile file : newImageFiles) {
                String newImageUrl = uploadFileToCloudinary(file);
                finalImageUrls.add(newImageUrl);
            }
        }

        // Logic to delete images that were removed on the frontend
        List<String> imagesToDelete = existing.getImages().stream()
                .filter(url -> !finalImageUrls.contains(url))
                .collect(Collectors.toList());

        for(String url : imagesToDelete) {
            cloudinaryService.deleteImageFromCloud(cloudinaryService.extractPublicId(url));
        }

        existing.setImages(finalImageUrls);

        return Optional.of(recipeRepository.save(existing));
    }

    public void deleteRecipe(Long chefId, Long recipeId) {
        // 1. Find the recipe to be deleted
        Recipe existing = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new RuntimeException("Recipe not found"));

        // 2. Authorization check
        if (!existing.getChef().getId().equals(chefId)) {
            throw new RuntimeException("Cannot delete another chef's recipe");
        }

        // 3. Delete the thumbnail from Cloudinary if it exists
        if (existing.getThumbnail() != null && !existing.getThumbnail().isEmpty()) {
            String publicId = cloudinaryService.extractPublicId(existing.getThumbnail());
            cloudinaryService.deleteImageFromCloud(publicId);
        }

        // 4. Delete all additional images from Cloudinary
        if (existing.getImages() != null && !existing.getImages().isEmpty()) {
            for (String imageUrl : existing.getImages()) {
                String publicId = cloudinaryService.extractPublicId(imageUrl);
                cloudinaryService.deleteImageFromCloud(publicId);
            }
        }

        // 5. Finally, delete the recipe record from your database
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

    // A NEW HELPER METHOD TO UPLOAD A FILE TO CLOUDINARY
    private String uploadFileToCloudinary(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        try {
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.emptyMap());
            return (String) uploadResult.get("secure_url");
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to upload file to Cloudinary");
        }
    }



    // ===== Add or Update Rating =====
    @Transactional
    public RatingResponseDTO addOrUpdateRating(Long recipeId, Long userId, int stars) {
        if (stars < 1 || stars > 5) {
            throw new RuntimeException("Rating must be between 1 and 5");
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

    public RatingStatsDTO getRatingStats(Long recipeId) {
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new RuntimeException("Recipe not found"));

        Double avg = ratingRepository.findAverageRatingByRecipe(recipe);
        List<Object[]> starCounts = ratingRepository.countRatingsByStars(recipe);

        long total = 0;
        Map<Integer, Long> countsMap = new HashMap<>();
        for (int i = 1; i <= 5; i++) countsMap.put(i, 0L); // initialize

        for (Object[] row : starCounts) {
            int star = (Integer) row[0];
            long count = (Long) row[1];
            countsMap.put(star, count);
            total += count;
        }

        return new RatingStatsDTO(avg != null ? avg : 0.0, total, countsMap);
    }


    // ===== Get average rating =====
    public double getAverageRating(Long recipeId) {
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new RuntimeException("Recipe not found"));
        Double avg = ratingRepository.findAverageRatingByRecipe(recipe);
        return avg != null ? avg : 0.0;
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

    // ===== Add or Update Review =====
    @Transactional
    public ReviewResponseDTO addOrUpdateReview(Long recipeId, Long userId, String reviewText) {
        if (reviewText == null || reviewText.isBlank()) {
            throw new RuntimeException("Review cannot be empty");
        }

        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new RuntimeException("Recipe not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        ReviewEntity review = reviewRepository.findByUserAndRecipe(user, recipe)
                .orElse(new ReviewEntity());

        review.setRecipe(recipe);
        review.setUser(user);
        review.setReviewText(reviewText);

        ReviewEntity savedReview = reviewRepository.save(review);
        return ReviewResponseDTO.fromEntity(savedReview);
    }

    // ===== Get all reviews for a recipe =====
    public List<ReviewResponseDTO> getReviewsForRecipe(Long recipeId) {
        List<ReviewEntity> reviews = reviewRepository.findByRecipe_RecipeId(recipeId);
        return reviews.stream()
                .map(ReviewResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }


    // ===== Delete a review =====
    public void deleteReview(Long userId, Long reviewId) {
        ReviewEntity review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found"));

        if (!review.getUser().getUserId().equals(userId)) {
            throw new RuntimeException("Cannot delete another user's review");
        }

        reviewRepository.delete(review);
    }


    public List<MostReviewedRecipeDTO> getChefMostReviewedRecipes(int limit, Long chefId) {
        Pageable pageable = PageRequest.of(0, limit);

        // Pass chefId to repository query
        List<Object[]> results = reviewRepository.findMostReviewedRecipesByChefId(chefId, pageable);

        List<MostReviewedRecipeDTO> dtoList = new ArrayList<>();
        for (Object[] result : results) {
            MostReviewedRecipeDTO dto = new MostReviewedRecipeDTO(
                    (Long) result[0],   // recipeId
                    (String) result[1], // title
                    (String) result[2], // thumbnail
                    (Long) result[3]    // totalReviews
            );
            dtoList.add(dto);
        }
        return dtoList;
    }


    //for chef homepage analytics
    @Transactional(readOnly = true)
    public ChefAnalyticsDTO getChefAnalytics(Long chefId) {

        // Total recipes
        int totalRecipes = recipeRepository.countByChefId(chefId);

        if (totalRecipes == 0) {
            return new ChefAnalyticsDTO(0, 0.0, 0.0);
        }

        // Fetch all recipes
        List<Recipe> recipes = recipeRepository.findByChefId(chefId);
        List<Long> recipeIds = recipes.stream().map(r -> r.getRecipeId()).toList();

        // Fetch all reviews
        List<ReviewEntity> reviews = reviewRepository.findByRecipe_RecipeIdIn(recipeIds);
        double avgReviews = (double) reviews.size() / totalRecipes;

        // Fetch all ratings
        List<RatingEntity> ratings = ratingRepository.findByRecipeIn(recipes);
        double avgRating = ratings.stream()
                .mapToInt(RatingEntity::getRatingValue)
                .average()
                .orElse(0.0);

        return new ChefAnalyticsDTO(totalRecipes, avgReviews, avgRating);
    }

    @Transactional(readOnly = true)
    public UserAnalyticsDTO getUserAnalytics(Long userId) {

        int totalRecipesTried = reviewRepository.countByUserId(userId);
        int totalRatingsGiven = ratingRepository.countByUserId(userId);

        // Get total favorites by loading the user and counting the Set size
        int totalFavoritesAdded = userRepository.findById(userId)
                .map(user -> user.getFavoriteRecipes().size())
                .orElse(0);

        return new UserAnalyticsDTO(totalRecipesTried, totalFavoritesAdded, totalRatingsGiven);
    }



    public List<MostReviewedRecipeDTO> getMostReviewedRecipes(int limit) {
        Pageable pageable = PageRequest.of(0, limit);

        // Fetch most-reviewed recipes without chef filter
        List<Object[]> results = reviewRepository.findMostReviewedRecipes(pageable);

        // Map results to DTOs
        return results.stream().map(result -> new MostReviewedRecipeDTO(
                (Long) result[0],   // recipeId
                (String) result[1], // title
                (String) result[2], // thumbnail
                (Long) result[3]    // totalReviews
        )).collect(Collectors.toList());
    }
}

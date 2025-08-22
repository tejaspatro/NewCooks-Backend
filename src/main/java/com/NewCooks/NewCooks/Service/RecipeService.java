package com.NewCooks.NewCooks.Service;

import com.NewCooks.NewCooks.Config.CloudinaryConfig;
import com.NewCooks.NewCooks.DTO.ChefDTO;
import com.NewCooks.NewCooks.DTO.RecipeDTO;
import com.NewCooks.NewCooks.DTO.RecipeResponseDTO;
import com.NewCooks.NewCooks.Entity.Chef;
import com.NewCooks.NewCooks.Entity.Recipe;
import com.NewCooks.NewCooks.Repository.ChefRepository;
import com.NewCooks.NewCooks.Repository.RecipeRepository;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
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
public class RecipeService {

    private final RecipeRepository recipeRepository;
    private final ChefRepository chefRepository;
    private final Cloudinary cloudinary;

    public RecipeService(RecipeRepository recipeRepository, ChefRepository chefRepository, CloudinaryConfig cloudinaryConfig) {
        this.recipeRepository = recipeRepository;
        this.chefRepository = chefRepository;
        this.cloudinary = cloudinaryConfig.getCloudinary();
    }

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
        ChefDTO chefDTO = new ChefDTO(chef.getId(), chef.getName(), chef.getEmail());
        return new RecipeResponseDTO(
                recipe.getRecipeId(),
                recipe.getTitle(),
                recipe.getDescription(),
                recipe.getIngredients(),
                recipe.getUtensils(),
                recipe.getNutritionInfo(),
                chefDTO,
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
}

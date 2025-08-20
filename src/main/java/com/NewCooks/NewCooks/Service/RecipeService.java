package com.NewCooks.NewCooks.Service;

import org.apache.catalina.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import com.NewCooks.NewCooks.DTO.ChefDTO;
import com.NewCooks.NewCooks.DTO.RecipeDTO;
import com.NewCooks.NewCooks.DTO.RecipeResponseDTO;
import com.NewCooks.NewCooks.Entity.Chef;
import com.NewCooks.NewCooks.Entity.Recipe;
import com.NewCooks.NewCooks.Repository.ChefRepository;
import com.NewCooks.NewCooks.Repository.RecipeRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class RecipeService {

    private final RecipeRepository recipeRepository;
    private final ChefRepository chefRepository;

    public RecipeService(RecipeRepository recipeRepository, ChefRepository chefRepository) {
        this.recipeRepository = recipeRepository;
        this.chefRepository = chefRepository;
    }

    public Recipe addRecipe(Long chefId, RecipeDTO dto) {
        Chef chef = chefRepository.findById(chefId)
                .orElseThrow(() -> new RuntimeException("Chef not found"));

        boolean exists = recipeRepository.existsByTitleAndChefId(dto.getTitle(), chefId);
        if (exists) {
            throw new RuntimeException("Recipe title already exists for this chef");
        }

        Recipe r = new Recipe();
        r.setTitle(dto.getTitle());
        r.setDescription(dto.getDescription());
        r.setChef(chef);
        r.setIngredients(dto.getIngredients());
        r.setUtensils(dto.getUtensils());
        r.setNutritionInfo(dto.getNutritionInfo());
        r.setInstructions(dto.getInstructions());
        return recipeRepository.save(r);
    }

    public Optional<Recipe> updateRecipe(Long chefId, Long recipeId, RecipeDTO dto) {
        Recipe existing = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new RuntimeException("Recipe not found"));
        if (!existing.getChef().getId().equals(chefId)) {
            throw new RuntimeException("Cannot update another chef's recipe");
        }

        if (!existing.getTitle().equals(dto.getTitle())) {
            boolean exists = recipeRepository.existsByTitleAndChefId(dto.getTitle(), chefId);
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
                recipe.getInstructions()
        );
    }

    // Method to get list of RecipeDTO by chefId
    public List<RecipeDTO> getRecipeDTOsByChef(Long chefId) {
        List<Recipe> recipes = recipeRepository.findByChefId(chefId);
        return recipes.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    public RecipeResponseDTO toRecipeResponseDTO(Recipe recipe)
    {
        Chef chef = recipe.getChef();
        ChefDTO chefDTO = new ChefDTO(chef.getId(), chef.getName(), chef.getEmail());
        return new RecipeResponseDTO(recipe.getRecipeId(), recipe.getTitle(), recipe.getDescription(), recipe.getIngredients(), recipe.getUtensils(), recipe.getNutritionInfo(), chefDTO, recipe.getInstructions());
    }
}

package com.NewCooks.NewCooks.Controller;

import com.NewCooks.NewCooks.DTO.RecipeResponseDTO;
import com.NewCooks.NewCooks.Entity.Recipe;
import com.NewCooks.NewCooks.Entity.User;
import com.NewCooks.NewCooks.Repository.UserRepository;
import com.NewCooks.NewCooks.Service.RecipeService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/user")
public class UserController {

    private final RecipeService recipeService;
    private final UserRepository userRepository;

    public UserController(RecipeService recipeService, UserRepository userRepository) {
        this.recipeService = recipeService;
        this.userRepository = userRepository;
    }

    @GetMapping("/recipes")
    public ResponseEntity<Page<RecipeResponseDTO>> viewAllRecipes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "16") int size
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

    @GetMapping("/test")
    public String test()
    {
        return "test successful";
    }

}


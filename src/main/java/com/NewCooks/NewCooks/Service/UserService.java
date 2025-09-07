package com.NewCooks.NewCooks.Service;

import com.NewCooks.NewCooks.DTO.FavoriteDTO;
import com.NewCooks.NewCooks.DTO.RecipeSearchSuggestionDTO;
import com.NewCooks.NewCooks.DTO.UserSignupDTO;
import com.NewCooks.NewCooks.Entity.Chef;
import com.NewCooks.NewCooks.Entity.Recipe;
import com.NewCooks.NewCooks.Entity.User;
import com.NewCooks.NewCooks.Repository.RecipeRepository;
import com.NewCooks.NewCooks.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.MailException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final RecipeRepository recipeRepository;

    public User registerUser(UserSignupDTO dto, String appBaseUrl) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new RuntimeException("Email already in use");
        }
        User user = new User();
        user.setName(dto.getName());
        user.setEmail(dto.getEmail());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setActive(false);
        user.setActivationToken(UUID.randomUUID().toString());
        User saved = userRepository.save(user);
        try {
            String activationLink = appBaseUrl + "/auth/activate?token=" + user.getActivationToken();
            emailService.sendActivationEmail(saved.getEmail(), activationLink);
        } catch (MailException e) {
            System.err.println("Failed to send activation email: " + e.getMessage());
            throw new RuntimeException("Registration successful but failed to send activation email. Please contact support.");
        }

        return saved;
    }

    public boolean activateUserByToken(String token) {
        Optional<User> userOpt = userRepository.findByActivationToken(token);
        if (userOpt.isEmpty()) {
            return false;
        }
        User user = userOpt.get();
        user.setActive(true);
        user.setActivationToken(null);
        userRepository.save(user);
        return true;
    }

    public User updateUserProfile(String email, String name, String profilePicture, String aboutMe) {
        // Find the user by email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Update only the fields we want
        if (name != null) {
            user.setName(name);
        }
        if (profilePicture != null) {
            user.setProfilePicture(profilePicture);
        }
        if (aboutMe != null) {
            user.setAboutMe(aboutMe);
        }

        // Save updated user
        return userRepository.save(user);
    }

    public List<RecipeSearchSuggestionDTO> searchRecipes(String keyword) {
        return recipeRepository.searchRecipesByKeyword(keyword)
                .stream()
                .map(RecipeSearchSuggestionDTO::fromEntity)
                .toList();
    }

    public FavoriteDTO toggleFavorite(String username, Long recipeId) {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new RuntimeException("Recipe not found"));

        boolean isFavorite;
        if (user.getFavoriteRecipes().contains(recipe)) {
            user.getFavoriteRecipes().remove(recipe);
            isFavorite = false;
        } else {
            user.getFavoriteRecipes().add(recipe);
            isFavorite = true;
        }

        userRepository.save(user);

        return new FavoriteDTO(recipeId, isFavorite);
    }

    public List<RecipeSearchSuggestionDTO> getUserFavorites(String username) {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return user.getFavoriteRecipes()
                .stream()
                .map(recipe -> new RecipeSearchSuggestionDTO(
                        recipe.getRecipeId(),
                        recipe.getTitle(),
                        recipe.getDescription() != null && recipe.getDescription().length() > 80
                                ? recipe.getDescription().substring(0, 80) + "..."
                                : recipe.getDescription()
                ))
                .toList();
    }


    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }
}

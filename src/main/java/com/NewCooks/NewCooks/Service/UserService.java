package com.NewCooks.NewCooks.Service;

import com.NewCooks.NewCooks.DTO.*;
import com.NewCooks.NewCooks.Entity.Chef;
import com.NewCooks.NewCooks.Entity.Recipe;
import com.NewCooks.NewCooks.Entity.User;
import com.NewCooks.NewCooks.Repository.RecipeRepository;
import com.NewCooks.NewCooks.Repository.UserRepository;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.MailException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final RecipeRepository recipeRepository;
    private final Cloudinary cloudinary;
    private final CloudinaryService cloudinaryService;
    private final RecipeService recipeService;

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

    public User updateUserProfile(String email, UserProfileDTO dto, MultipartFile profilePictureFile) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Update basic fields
        user.setName(dto.getName());
        user.setAboutMe(dto.getAboutMe());

        // Handle profile picture update
        // Case 1: A new file is uploaded
        if (profilePictureFile != null && !profilePictureFile.isEmpty()) {
            // Delete old picture from Cloudinary if it exists
            if (user.getProfilePicture() != null && !user.getProfilePicture().isEmpty()) {
                String publicId = cloudinaryService.extractPublicId(user.getProfilePicture());
                cloudinaryService.deleteImageFromCloud(publicId);
            }
            // Upload new picture and set URL
            String newProfilePictureUrl = uploadFileToCloudinary(profilePictureFile);
            user.setProfilePicture(newProfilePictureUrl);
        }
        // Case 2: User removed picture without uploading a new one
        else if (dto.getProfilePicture() == null || dto.getProfilePicture().isEmpty()) {
            if (user.getProfilePicture() != null && !user.getProfilePicture().isEmpty()) {
                String publicId = cloudinaryService.extractPublicId(user.getProfilePicture());
                cloudinaryService.deleteImageFromCloud(publicId);
                user.setProfilePicture(null);
            }
        }

        return userRepository.save(user);
    }

    private String uploadFileToCloudinary(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        try {
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.emptyMap());
            return (String) uploadResult.get("secure_url");
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload file to Cloudinary", e);
        }
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

    public List<RecipeResponseDTO> getUserFavorites(String username) {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return user.getFavoriteRecipes()
                .stream()
                .map(recipe -> recipeService.toRecipeResponseDTO(recipe)) // reuse existing mapping
                .toList();
    }



    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }
}

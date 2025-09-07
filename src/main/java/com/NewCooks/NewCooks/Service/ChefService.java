package com.NewCooks.NewCooks.Service;

import com.NewCooks.NewCooks.DTO.ChefProfileDTO;
import com.NewCooks.NewCooks.DTO.ChefRecipeSearchSuggestionDTO;
import com.NewCooks.NewCooks.DTO.ChefSignupDTO;
import com.NewCooks.NewCooks.Entity.Chef;
import com.NewCooks.NewCooks.Repository.ChefRepository;
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
import java.util.UUID;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ChefService {

    private final ChefRepository chefRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final CloudinaryService cloudinaryService;
    private final Cloudinary cloudinary;

    public Chef registerChef(ChefSignupDTO dto, String appBaseUrl) {
        if (chefRepository.existsByEmail(dto.getEmail())) {
            throw new RuntimeException("Email already in use");
        }

        Chef chef = new Chef();
        chef.setName(dto.getName());
        chef.setEmail(dto.getEmail());
        chef.setPassword(passwordEncoder.encode(dto.getPassword()));
        chef.setActive(false);
        chef.setActivationToken(UUID.randomUUID().toString());
        Chef saved = chefRepository.save(chef);
        String link = appBaseUrl + "/auth/activate?token=" + chef.getActivationToken();
        try {
            emailService.sendActivationEmail(saved.getEmail(), link);
        } catch (MailException e) {
            System.err.println("Failed to send activation email: " + e.getMessage());
            throw new RuntimeException("Registration successful but failed to send activation email. Please contact support.");
        }

        return saved;
    }

    public boolean activateChefByToken(String token) {
        Optional<Chef> chefOpt = chefRepository.findByActivationToken(token);
        if (chefOpt.isEmpty()) {
            return false;
        }
        Chef chef = chefOpt.get();
        chef.setActive(true);
        chef.setActivationToken(null);
        chefRepository.save(chef);
        return true;
    }

    public ChefProfileDTO toChefProfileDTO(Chef chef) {
        return new ChefProfileDTO(
                chef.getId(),
                chef.getName(),
                chef.getEmail(),
                chef.getExpertise(),
                chef.getExperience(),
                chef.getBio(),
                chef.getProfilePicture()
        );
    }

    public ChefProfileDTO getChefProfile(String email) {
        Chef chef = chefRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Chef not found"));
        return toChefProfileDTO(chef);
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


    public Chef updateChefProfile(String email, ChefProfileDTO dto, MultipartFile profilePictureFile) {
        Chef chef = chefRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Chef not found"));

        chef.setName(dto.getName());
        chef.setExpertise(dto.getExpertise());
        chef.setExperience(dto.getExperience());
        chef.setBio(dto.getBio());

        // Handle profile picture update
        // Case 1: A new file is uploaded.
        if (profilePictureFile != null && !profilePictureFile.isEmpty()) {
            // Delete the old picture from Cloudinary if it exists
            if (chef.getProfilePicture() != null && !chef.getProfilePicture().isEmpty()) {
                String publicId = cloudinaryService.extractPublicId(chef.getProfilePicture()); // Assuming extractPublicId helper exists
                cloudinaryService.deleteImageFromCloud(publicId); // Assuming deleteImageFromCloud helper exists
            }
            // Upload the new picture and set the URL
            String newProfilePictureUrl = uploadFileToCloudinary(profilePictureFile);
            chef.setProfilePicture(newProfilePictureUrl);
        }
        // Case 2: The user removed the picture without uploading a new one.
        else if (dto.getProfilePicture() == null || dto.getProfilePicture().isEmpty()) {
            if (chef.getProfilePicture() != null && !chef.getProfilePicture().isEmpty()) {
                String publicId = cloudinaryService.extractPublicId(chef.getProfilePicture());
                cloudinaryService.deleteImageFromCloud(publicId);
                chef.setProfilePicture(null); // Set to null in the database
            }
        }

        return chefRepository.save(chef);
    }
    public List<ChefRecipeSearchSuggestionDTO> searchChefRecipes(Long chefId, String keyword) {
        return chefRepository.searchRecipesByChefAndKeyword(chefId, keyword)
                .stream()
                .map(r -> new ChefRecipeSearchSuggestionDTO(r.getRecipeId(), r.getTitle(), r.getDescription()))
                .toList();
    }



    public Optional<Chef> findByEmail(String email) {
        return chefRepository.findByEmail(email);
    }

    public Optional<Chef> findById(Long chefId) {
        return chefRepository.findById(chefId);
    }
}

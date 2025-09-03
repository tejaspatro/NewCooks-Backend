package com.NewCooks.NewCooks.Service;

import com.NewCooks.NewCooks.DTO.ChefProfileDTO;
import com.NewCooks.NewCooks.DTO.ChefRecipeSearchSuggestionDTO;
import com.NewCooks.NewCooks.DTO.ChefSignupDTO;
import com.NewCooks.NewCooks.Entity.Chef;
import com.NewCooks.NewCooks.Repository.ChefRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.MailException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ChefService {

    private final ChefRepository chefRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

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

    public Chef updateChefProfile(String email, String name, String expertise, String experience, String bio, String profilePicture) {
        // Find the chef by email
        Chef chef = chefRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Chef not found"));

        // Update only the fields we want
        if (name != null) {
            chef.setName(name);
        }
        if (expertise != null) chef.setExpertise(expertise);
        if (experience != null) chef.setExperience(experience);
        if (bio != null) chef.setBio(bio);
        if (profilePicture != null) chef.setProfilePicture(profilePicture);

        // Save updated chef
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
}

package com.NewCooks.NewCooks.Service;

import com.NewCooks.NewCooks.DTO.UserSignupDTO;
import com.NewCooks.NewCooks.Entity.Chef;
import com.NewCooks.NewCooks.Entity.User;
import com.NewCooks.NewCooks.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.MailException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

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

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }
}

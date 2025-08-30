package com.NewCooks.NewCooks.Controller;

import com.NewCooks.NewCooks.DTO.*;
import com.NewCooks.NewCooks.Entity.Chef;
import com.NewCooks.NewCooks.Entity.User;
import com.NewCooks.NewCooks.Repository.ChefRepository;
import com.NewCooks.NewCooks.Repository.UserRepository;
import com.NewCooks.NewCooks.Security.JWTUtil;
import com.NewCooks.NewCooks.Service.ChefService;
import com.NewCooks.NewCooks.Service.EmailService;
import com.NewCooks.NewCooks.Service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "http://localhost:5173")
public class AuthController {

    private final ChefService chefService;
    private final UserService userService;
    private final ChefRepository chefRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JWTUtil jwtUtil;
    @Autowired
    private EmailService emailService;

    @Value("${app.base-url:http://localhost:8080}")
    private String appBaseUrl;

    public AuthController(ChefService chefService,
                          UserService userService,
                          ChefRepository chefRepository,
                          UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          JWTUtil jwtUtil) {
        this.chefService = chefService;
        this.userService = userService;
        this.chefRepository = chefRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/chef/register")
    public ResponseEntity<?> registerChef(@RequestBody ChefSignupDTO dto,
                                          @RequestHeader(value = "origin", required = false) String origin) {
        try {
            String base = appBaseUrl;
            if (!base.endsWith("/newcooks")) {
                base = base.endsWith("/") ? base + "newcooks" : base + "/newcooks";
            }
            Chef saved = chefService.registerChef(dto, base);
            saved.setPassword(null);
            return ResponseEntity.ok(saved);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/chef/login")
    public ResponseEntity<?> loginChef(@RequestBody ChefLoginRequest req) {
        return chefRepository.findByEmail(req.getEmail())
                .map(chef -> {
                    if (!chef.isActive()) {
                        return ResponseEntity.status(403)
                                .body(Map.of("message", "Please activate your account first"));
                    }
                    if (passwordEncoder.matches(req.getPassword(), chef.getPassword())) {
                        String token = jwtUtil.generateToken(chef.getEmail(), "ROLE_CHEF");
                        return ResponseEntity.ok(
                                Map.of(
                                        "token", token,
                                        "role", "chef",
                                        "user", getChefProfile(chef)
                                )
                        );
                    } else {
                        return ResponseEntity.status(401)
                                .body(Map.of("message", "Invalid password"));
                    }
                }).orElse(ResponseEntity.status(404)
                        .body(Map.of("message", "Email not registered. Please sign up")));
    }

    @PostMapping("/user/register")
    public ResponseEntity<?> registerUser(@RequestBody UserSignupDTO dto,
                                          @RequestHeader(value = "origin", required = false) String origin) {
        try {
            String base = appBaseUrl;
            if (!base.endsWith("/newcooks")) {
                base = base.endsWith("/") ? base + "newcooks" : base + "/newcooks";
            }
            User saved = userService.registerUser(dto, base);
            saved.setPassword(null);
            return ResponseEntity.ok(saved);
        }catch (RuntimeException e)
        {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/user/login")
    public ResponseEntity<?> loginUser(@RequestBody UserLoginRequest req) {
        return userRepository.findByEmail(req.getEmail())
                .map(user -> {
                    if (!user.isActive()) {
                        return ResponseEntity.status(403)
                                .body(Map.of("message", "Please activate your account first"));
                    }
                    if (passwordEncoder.matches(req.getPassword(), user.getPassword())) {
                        String token = jwtUtil.generateToken(user.getEmail(), "ROLE_USER");
                        return ResponseEntity.ok(
                                Map.of(
                                        "token", token,
                                        "role", "user",
                                        "user", getUserProfile(user)
                                )
                        );
                    } else {
                        return ResponseEntity.status(401)
                                .body(Map.of("message", "Invalid password"));
                    }
                }).orElse(ResponseEntity.status(404)
                        .body(Map.of("message", "Email not registered. Please sign up")));
    }

    private Map<String, Object> getUserProfile(User user) {
        return Map.of(
                "id", user.getUserId(),
                "name", user.getName(),
                "email", user.getEmail()
        );
    }


    private Map<String, Object> getChefProfile(Chef chef) {
        return Map.of(
                "id", chef.getId(),
                "name", chef.getName(),
                "email", chef.getEmail()
//                "recipes", chef.getRecipes()
        );
    }

    // Activation endpoint for both User and Chef activation tokens
    @GetMapping("/activate")
    public ResponseEntity<String> activateAccount(@RequestParam("token") String token) {
        boolean activatedUser = userService.activateUserByToken(token);
        boolean activatedChef = chefService.activateChefByToken(token);

        if (activatedUser || activatedChef) {
            return ResponseEntity.ok("Account activated successfully");
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid or expired activation token");
        }
    }

    @PostMapping("/user/forgot-password")
    public ResponseEntity<?> forgotUserPassword(@RequestBody Map<String, String> req,
                                                @RequestHeader(value = "origin", required = false) String origin) {
        String email = req.get("email");
        String newPassword = req.get("newPassword");

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return ResponseEntity.status(404).body(Map.of("message", "User not found"));
        }

        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            return ResponseEntity.badRequest().body(Map.of("message", "New password cannot be same as old password"));
        }

        // === Send activation email automatically ===
        String token = UUID.randomUUID().toString();  // generate token
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setActive(false); // Require reactivation
        user.setActivationToken(token);
        userRepository.save(user);

        try {
            String base = appBaseUrl;
            if (!base.endsWith("/newcooks")) {
                base = base.endsWith("/") ? base + "newcooks" : base + "/newcooks";
            }
            String activationLink = base + "/auth/activate?token=" + token;
            emailService.sendActivationEmail(user.getEmail(),activationLink);
        } catch (MailException e) {
            System.err.println("Failed to send activation email: " + e.getMessage());
            throw new RuntimeException("Registration successful but failed to send activation email. Please contact support.");
        }
        return ResponseEntity.ok(Map.of("message", "Password updated. Activation email sent to your inbox."));
    }


    @PostMapping("/chef/forgot-password")
    public ResponseEntity<?> forgotChefPassword(@RequestBody Map<String, String> req,
                                                @RequestHeader(value = "origin", required = false) String origin) {
        String email = req.get("email");
        String newPassword = req.get("newPassword");

        Chef chef = chefRepository.findByEmail(email).orElse(null);
        if (chef == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Chef not found"));
        }

        if (passwordEncoder.matches(newPassword, chef.getPassword())) {
            return ResponseEntity.badRequest().body(Map.of("message", "New password cannot be same as old password"));
        }

        // === Send activation email automatically ===
        String token = UUID.randomUUID().toString();  // generate token
        chef.setPassword(passwordEncoder.encode(newPassword));
        chef.setActive(false); // Require reactivation
        chef.setActivationToken(token);
        chefRepository.save(chef);

        try {
            String base = appBaseUrl;
            if (!base.endsWith("/newcooks")) {
                base = base.endsWith("/") ? base + "newcooks" : base + "/newcooks";
            }
            String activationLink = base + "/auth/activate?token=" + token;
            emailService.sendActivationEmail(chef.getEmail(),activationLink);
        } catch (MailException e) {
            System.err.println("Failed to send activation email: " + e.getMessage());
            throw new RuntimeException("Registration successful but failed to send activation email. Please contact support.");
        }
        return ResponseEntity.ok(Map.of("message", "Password updated. Activation email sent to your inbox."));
    }

    @GetMapping("/test")
    public String test() {
        return "Test Successful";
    }
}

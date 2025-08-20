package com.NewCooks.NewCooks.Security;

import com.NewCooks.NewCooks.Entity.Chef;
import com.NewCooks.NewCooks.Entity.User;
import com.NewCooks.NewCooks.Repository.ChefRepository;
import com.NewCooks.NewCooks.Repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final ChefRepository chefRepo;
    private final UserRepository userRepo;

    public CustomUserDetailsService(ChefRepository chefRepo, UserRepository userRepo) {
        this.chefRepo = chefRepo;
        this.userRepo = userRepo;
    }

    @Override
    public org.springframework.security.core.userdetails.UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        // try chef
        Chef chef = chefRepo.findByEmail(email).orElse(null);
        if (chef != null) {
            if (!chef.isActive()) {
                throw new UsernameNotFoundException("Chef not activated");
            }
            return org.springframework.security.core.userdetails.User
                    .withUsername(chef.getEmail())
                    .password(chef.getPassword())
                    .authorities("ROLE_CHEF")
                    .accountLocked(false)
                    .build();
        }

        // try user
        User user = userRepo.findByEmail(email).orElse(null);
        if (user != null) {
            if (!user.isActive()) {
                throw new UsernameNotFoundException("User not activated");
            }
            return org.springframework.security.core.userdetails.User
                    .withUsername(user.getEmail())
                    .password(user.getPassword())
                    .authorities("ROLE_USER")
                    .accountLocked(false)
                    .build();
        }

        throw new UsernameNotFoundException("User not found with email: " + email);
    }
}

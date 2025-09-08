//package com.NewCooks.NewCooks.Config;
//
//import com.NewCooks.NewCooks.Security.CustomUserDetailsService;
//import com.NewCooks.NewCooks.Security.JWTAuthenticationFilter;
//import com.NewCooks.NewCooks.Security.JWTUtil;
//import lombok.RequiredArgsConstructor;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
//import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
//import org.springframework.security.config.annotation.web.builders.HttpSecurity;
//import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
//import org.springframework.security.web.SecurityFilterChain;
//import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
//import org.springframework.web.cors.CorsConfiguration;
//import org.springframework.web.servlet.config.annotation.CorsRegistry;
//import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
//
//import java.util.List;
//
//@Configuration
//@EnableWebSecurity
//@EnableMethodSecurity
//@RequiredArgsConstructor
//public class SecurityConfig {
//
//    private final CustomUserDetailsService userDetailsService;
//    private final JWTUtil jwtUtil;
//
//    @Bean
//    public BCryptPasswordEncoder passwordEncoder() {
//        return new BCryptPasswordEncoder();
//    }
//
//    @Bean
//    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
//        JWTAuthenticationFilter jwtFilter = new JWTAuthenticationFilter(jwtUtil);
//
//        http
//                .csrf(csrf -> csrf.disable())
//                .authorizeHttpRequests(auth -> auth
//                        .requestMatchers("/auth/**", "/activate/**", "/h2-console/**","/test").permitAll()
//                        .anyRequest().authenticated()
//                )
//                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
//
//        // Allow frames for H2 console in dev
//        http.headers(headers -> headers.frameOptions(frame -> frame.disable()));
//
//        return http.build();
//    }
//
//    @Bean
//    public WebMvcConfigurer corsConfigurer() {
//        return new WebMvcConfigurer() {
//            @Override
//            public void addCorsMappings(CorsRegistry registry) {
//                registry.addMapping("/**")              // Allow all endpoints
//                        .allowedOrigins("*")            // Allow all origins, or specify your frontend URL here
//                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
//                        .allowedHeaders("*")
//                        .allowCredentials(false);      // Set true if you want to support credentials like cookies or auth headers
//            }
//        };
//    }
//
//}
package com.NewCooks.NewCooks.Config;

import com.NewCooks.NewCooks.Security.CustomUserDetailsService;
import com.NewCooks.NewCooks.Security.JWTAuthenticationFilter;
import com.NewCooks.NewCooks.Security.JWTUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder; // import this!
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    // These are injected by Spring using @RequiredArgsConstructor
    private final CustomUserDetailsService userDetailsService;
    private final JWTUtil jwtUtil;

    // This bean gets injected everywhere PasswordEncoder is needed!
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Value("${newcooks.frontend.url}")
    private String frontendurl;

    // Strong CORS setup: allow only your frontend port!
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(frontendurl)); // Only allow your React frontend
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(false); // If you use cookies, set true

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        JWTAuthenticationFilter jwtFilter = new JWTAuthenticationFilter(jwtUtil);

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource())) // enable above CORS
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/**", "/activate/**", "/h2-console/**", "/test", "/error", "/user/recipes", "/recipes/rating/**").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        // Allow frames for H2 console in dev (for learning/DB browser)
        http.headers(headers -> headers.frameOptions(frame -> frame.disable()));

        return http.build();
    }
}


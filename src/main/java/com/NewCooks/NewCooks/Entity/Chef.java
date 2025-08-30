package com.NewCooks.NewCooks.Entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tbl_chef")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Chef {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private boolean active = false; // For email activation

    @OneToMany(mappedBy = "chef", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Recipe> recipes = new ArrayList<>();

    @Column(unique = true)
    private String activationToken;

    @Column(length = 255)
    private String expertise;

    @Column(length = 255)
    private String experience; // Can also be an integer if storing years

    @Column(length = 1000)
    private String bio;

    @Column(name = "profile_picture")
    private String profilePicture;


}

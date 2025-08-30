package com.NewCooks.NewCooks.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChefProfileDTO {
    private Long id;
    private String name;
    private String email;
    private String expertise;
    private String experience;
    private String bio;
    private String profilePicture;
}

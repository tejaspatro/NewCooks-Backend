package com.NewCooks.NewCooks.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileDTO {
    private Long userId;
    private String name;
    private String email;
    private String profilePicture;
    private String aboutMe;
}

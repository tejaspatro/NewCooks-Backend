package com.NewCooks.NewCooks.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.management.relation.Role;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChefDTO
{
    private Long id;
    private String name;
    private String email;
}

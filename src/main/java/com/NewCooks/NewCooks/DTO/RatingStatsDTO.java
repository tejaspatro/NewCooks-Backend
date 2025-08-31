package com.NewCooks.NewCooks.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RatingStatsDTO {
    private double average;
    private long total;
    private Map<Integer, Long> counts;
}

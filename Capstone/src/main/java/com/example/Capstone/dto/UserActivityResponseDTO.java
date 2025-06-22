package com.example.Capstone.dto;

import com.example.Capstone.entity.UserActivity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserActivityResponseDTO {
    private Long id;
    private UserActivity.ActivityType activityType;
    private String description;
    private LocalDateTime createdAt;
    private Long userId;
    private String userName;
    private Long performedByUserId;
    private String performedByUserName;
}
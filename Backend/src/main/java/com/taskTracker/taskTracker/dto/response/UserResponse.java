package com.taskTracker.taskTracker.dto.response;

import com.taskTracker.taskTracker.entity.Role;

public record UserResponse(Long id, String email, String fullName, Role role) {}

package com.taskTracker.taskTracker.dto.response;

import com.taskTracker.taskTracker.entity.Role;

public record AuthResponse(String token, Long id, String email, String fullName, Role role) {}

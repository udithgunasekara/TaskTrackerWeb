package com.taskTracker.taskTracker.common.payload.response;

import com.taskTracker.taskTracker.common.type.Role;

public record AuthResponse(String token, Long id, String email, String fullName, Role role) {}

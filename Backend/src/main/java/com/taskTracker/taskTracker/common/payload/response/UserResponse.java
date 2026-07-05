package com.taskTracker.taskTracker.common.payload.response;

import com.taskTracker.taskTracker.common.type.Role;

public record UserResponse(Long id, String email, String fullName, Role role) {}

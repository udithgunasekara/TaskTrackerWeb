package com.taskTracker.taskTracker.dto.response;

import com.taskTracker.taskTracker.entity.TaskStatus;
import java.time.Instant;
import java.time.LocalDate;

public record TaskResponse(
    Long id,
    String title,
    String description,
    TaskStatus status,
    LocalDate dueDate,
    UserResponse owner,
    Instant createdAt,
    Instant updatedAt) {}

package com.taskTracker.taskTracker.task.payload.response;

import com.taskTracker.taskTracker.common.payload.response.UserResponse;
import com.taskTracker.taskTracker.task.type.TaskStatus;
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

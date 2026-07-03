package com.taskTracker.taskTracker.dto.request;

import com.taskTracker.taskTracker.entity.TaskStatus;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record TaskRequest(
    @NotBlank @Size(max = 150) String title,
    @Size(max = 2000) String description,
    @NotNull TaskStatus status,
    @FutureOrPresent LocalDate dueDate) {}

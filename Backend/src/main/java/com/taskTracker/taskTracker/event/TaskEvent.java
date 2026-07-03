package com.taskTracker.taskTracker.event;

import com.taskTracker.taskTracker.dto.response.TaskResponse;

public record TaskEvent(TaskEventType type, TaskResponse task) {}

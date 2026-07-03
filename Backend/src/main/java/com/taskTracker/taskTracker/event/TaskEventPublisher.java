package com.taskTracker.taskTracker.event;

import com.taskTracker.taskTracker.dto.response.TaskResponse;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class TaskEventPublisher {

  private final SimpMessagingTemplate messagingTemplate;

  public TaskEventPublisher(SimpMessagingTemplate messagingTemplate) {
    this.messagingTemplate = messagingTemplate;
  }

  public void publishEvent(TaskEventType type, TaskResponse task) {
    TaskEvent event = new TaskEvent(type, task);

    // Publish to the specific user's queue
    if (task.owner() != null && task.owner().email() != null) {
      messagingTemplate.convertAndSendToUser(task.owner().email(), "/queue/tasks", event);
    }

    // Publish to the admin topic
    messagingTemplate.convertAndSend("/topic/admin/tasks", event);
  }
}

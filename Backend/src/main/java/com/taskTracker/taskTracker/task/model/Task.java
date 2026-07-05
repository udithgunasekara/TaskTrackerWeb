package com.taskTracker.taskTracker.task.model;

import com.taskTracker.taskTracker.common.model.User;
import com.taskTracker.taskTracker.task.type.TaskStatus;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "tasks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Task {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 150)
  private String title;

  @Column(columnDefinition = "TEXT")
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private TaskStatus status;

  private LocalDate dueDate;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "owner_id")
  private User owner;

  @CreatedDate
  @Column(updatable = false)
  private Instant createdAt;

  @LastModifiedDate private Instant updatedAt;
}

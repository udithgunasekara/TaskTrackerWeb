package com.taskTracker.taskTracker.task.repository;

import com.taskTracker.taskTracker.task.model.Task;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskDao extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {
  Optional<Task> findByIdAndOwnerId(Long id, Long ownerId);
}

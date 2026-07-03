package com.taskTracker.taskTracker.repository;

import com.taskTracker.taskTracker.entity.Task;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {
  Optional<Task> findByIdAndOwnerId(Long id, Long ownerId);
}

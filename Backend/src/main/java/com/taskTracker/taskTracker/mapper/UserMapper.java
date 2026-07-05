package com.taskTracker.taskTracker.mapper;

import com.taskTracker.taskTracker.dto.response.UserResponse;
import com.taskTracker.taskTracker.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
  UserResponse toResponse(User user);
}

package com.taskTracker.taskTracker.common.mapper;

import com.taskTracker.taskTracker.common.model.User;
import com.taskTracker.taskTracker.common.payload.response.UserResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
  UserResponse toResponse(User user);
}

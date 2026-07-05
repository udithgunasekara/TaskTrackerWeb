package com.taskTracker.taskTracker.common.service;

import com.taskTracker.taskTracker.common.payload.request.LoginRequest;
import com.taskTracker.taskTracker.common.payload.request.RegisterRequest;
import com.taskTracker.taskTracker.common.payload.response.AuthResponse;

public interface AuthService {
  AuthResponse register(RegisterRequest request);

  AuthResponse login(LoginRequest request);
}

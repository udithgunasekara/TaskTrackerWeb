package com.taskTracker.taskTracker.service.impl;

import com.taskTracker.taskTracker.dto.request.LoginRequest;
import com.taskTracker.taskTracker.dto.request.RegisterRequest;
import com.taskTracker.taskTracker.dto.response.AuthResponse;
import com.taskTracker.taskTracker.entity.Role;
import com.taskTracker.taskTracker.entity.User;
import com.taskTracker.taskTracker.repository.UserRepository;
import com.taskTracker.taskTracker.security.JwtUtil;
import com.taskTracker.taskTracker.service.AuthService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtUtil jwtUtil;
  private final AuthenticationManager authenticationManager;

  public AuthServiceImpl(
      UserRepository userRepository,
      PasswordEncoder passwordEncoder,
      JwtUtil jwtUtil,
      AuthenticationManager authenticationManager) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtUtil = jwtUtil;
    this.authenticationManager = authenticationManager;
  }

  @Override
  public AuthResponse register(RegisterRequest request) {
    if (userRepository.existsByEmail(request.email())) {
      throw new com.taskTracker.taskTracker.exception.DuplicateEmailException(
          "Email already in use");
    }

    User user =
        User.builder()
            .email(request.email())
            .password(passwordEncoder.encode(request.password()))
            .fullName(request.fullName())
            .role(Role.USER) // default role
            .build();

    userRepository.save(user);

    String token = jwtUtil.generateToken(user);
    return new AuthResponse(
        token, user.getId(), user.getEmail(), user.getFullName(), user.getRole());
  }

  @Override
  public AuthResponse login(LoginRequest request) {
    authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(request.email(), request.password()));

    User user =
        userRepository
            .findByEmail(request.email())
            .orElseThrow(
                () ->
                    new org.springframework.security.authentication.BadCredentialsException(
                        "Invalid email or password"));

    String token = jwtUtil.generateToken(user);
    return new AuthResponse(
        token, user.getId(), user.getEmail(), user.getFullName(), user.getRole());
  }
}

package com.taskTracker.taskTracker.common.config;

import com.taskTracker.taskTracker.common.model.User;
import com.taskTracker.taskTracker.common.repository.UserDao;
import com.taskTracker.taskTracker.common.type.Role;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Configuration
public class AdminSeeder {

  @Value("${app.admin.email}")
  private String adminEmail;

  @Value("${app.admin.password}")
  private String adminPassword;

  @Value("${app.admin.full-name}")
  private String adminFullName;

  @Bean
  public CommandLineRunner seedAdmin(UserDao userDao) {
    return args -> {
      if (!userDao.existsByEmail(adminEmail)) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        User admin =
            User.builder()
                .email(adminEmail)
                .password(encoder.encode(adminPassword))
                .fullName(adminFullName)
                .role(Role.ADMIN)
                .build();
        userDao.save(admin);
      }
    };
  }
}

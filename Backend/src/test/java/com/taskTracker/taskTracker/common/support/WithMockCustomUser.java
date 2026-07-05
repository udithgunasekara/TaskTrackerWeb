package com.taskTracker.taskTracker.common.support;

import com.taskTracker.taskTracker.common.type.Role;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.security.test.context.support.WithSecurityContext;

/**
 * Like {@code @WithMockUser}, but puts a real {@link
 * com.taskTracker.taskTracker.common.security.CustomUserDetails} on the security context so
 * controllers that read {@code @AuthenticationPrincipal CustomUserDetails} resolve a non-null
 * principal.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@WithSecurityContext(factory = WithMockCustomUserSecurityContextFactory.class)
public @interface WithMockCustomUser {
  long id() default 1L;

  String email() default "user@test.com";

  Role role() default Role.USER;
}

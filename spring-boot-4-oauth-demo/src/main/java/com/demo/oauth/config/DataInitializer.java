package com.demo.oauth.config;

import com.demo.oauth.entity.AppUser;
import com.demo.oauth.entity.GroupMember;
import com.demo.oauth.entity.PermissionGroup;
import com.demo.oauth.service.AppUserService;
import com.demo.oauth.service.GroupMemberService;
import com.demo.oauth.service.PermissionGroupService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds the default permission groups (ADMIN / USER) and demo users on first
 * start, via MyBatis-Plus services. Runs after sql-init has created the tables.
 */
@Component
@Order(0)
public class DataInitializer implements CommandLineRunner {

  private final PermissionGroupService groupService;
  private final AppUserService userService;
  private final GroupMemberService memberService;
  private final PasswordEncoder passwordEncoder;

  public DataInitializer(PermissionGroupService groupService, AppUserService userService,
      GroupMemberService memberService, PasswordEncoder passwordEncoder) {
    this.groupService = groupService;
    this.userService = userService;
    this.memberService = memberService;
    this.passwordEncoder = passwordEncoder;
  }

  @Override
  @Transactional
  public void run(String... args) {
    seedGroup("ADMIN", "管理员组：可访问管理接口");
    seedGroup("USER", "普通用户组：默认权限");
    seedUser("admin", "admin", "管理员", "ADMIN", "USER");
    seedUser("demo", "demo", "演示用户", "USER");
  }

  private void seedGroup(String name, String description) {
    if (!groupService.lambdaQuery().eq(PermissionGroup::getName, name).exists()) {
      groupService.save(new PermissionGroup(name, description));
    }
  }

  private void seedUser(String username, String rawPassword, String displayName,
      String... groups) {
    if (userService.findByUsername(username) == null) {
      userService.save(new AppUser(username, passwordEncoder.encode(rawPassword), displayName, true));
    }
    for (String group : groups) {
      if (!memberService.membershipExists(group, username)) {
        memberService.save(new GroupMember(group, username));
      }
    }
  }
}

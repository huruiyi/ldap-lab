package com.demo.oauth.controller;

import com.demo.oauth.entity.AppUser;
import com.demo.oauth.entity.GroupMember;
import com.demo.oauth.entity.PermissionGroup;
import com.demo.oauth.service.AppUserService;
import com.demo.oauth.service.GroupMemberService;
import com.demo.oauth.service.PermissionGroupService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * User administration (ADMIN group only). The bootstrap account "admin" cannot
 * be disabled or deleted, so an operator can never lock themselves out.
 */
@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

  private static final String PROTECTED_USER = "admin";

  private final AppUserService userService;
  private final GroupMemberService memberService;
  private final PermissionGroupService groupService;
  private final PasswordEncoder passwordEncoder;

  public AdminUserController(AppUserService userService, GroupMemberService memberService,
      PermissionGroupService groupService, PasswordEncoder passwordEncoder) {
    this.userService = userService;
    this.memberService = memberService;
    this.groupService = groupService;
    this.passwordEncoder = passwordEncoder;
  }

  @GetMapping
  public List<Map<String, Object>> listUsers() {
    return userService.list().stream().map(user -> {
      Map<String, Object> item = new LinkedHashMap<>();
      item.put("username", user.getUsername());
      item.put("displayName", user.getDisplayName());
      item.put("enabled", user.getEnabled());
      item.put("groups", memberService.groupsOf(user.getUsername()));
      item.put("protected", PROTECTED_USER.equals(user.getUsername()));
      return item;
    }).toList();
  }

  @PostMapping
  @Transactional
  public ResponseEntity<Map<String, String>> createUser(@RequestBody Map<String, Object> body) {
    String username = str(body.get("username")).trim();
    String password = str(body.get("password"));
    String displayName = str(body.get("displayName")).trim();
    if (!username.matches("^[a-zA-Z][a-zA-Z0-9_.-]{1,29}$")) {
      return bad("用户名需为 2-30 位字母/数字/_.-，且以字母开头");
    }
    if (password.length() < 4) {
      return bad("密码至少 4 位");
    }
    if (userService.findByUsername(username) != null) {
      return bad("用户名已存在: " + username);
    }
    userService.save(new AppUser(username, passwordEncoder.encode(password), displayName, true));
    String groupError = replaceGroups(username, groups(body));
    if (groupError != null) {
      return bad(groupError);
    }
    return ResponseEntity.ok(Map.of("message", "已创建用户 " + username));
  }

  @PutMapping("/{username}")
  @Transactional
  public ResponseEntity<Map<String, String>> updateUser(@PathVariable String username,
      @RequestBody Map<String, Object> body) {
    AppUser user = userService.findByUsername(username);
    if (user == null) {
      return bad("用户不存在: " + username);
    }
    boolean enabled = !body.containsKey("enabled") || Boolean.TRUE.equals(body.get("enabled"));
    if (PROTECTED_USER.equals(username) && !enabled) {
      return bad("内置管理员 admin 不可禁用");
    }
    user.setDisplayName(str(body.get("displayName")).trim());
    user.setEnabled(enabled);
    userService.updateById(user);
    String groupError = replaceGroups(username, groups(body));
    if (groupError != null) {
      return bad(groupError);
    }
    return ResponseEntity.ok(Map.of("message", "已更新用户 " + username));
  }

  @PostMapping("/{username}/password")
  public ResponseEntity<Map<String, String>> resetPassword(@PathVariable String username,
      @RequestBody Map<String, Object> body) {
    AppUser user = userService.findByUsername(username);
    if (user == null) {
      return bad("用户不存在: " + username);
    }
    String password = str(body.get("password"));
    if (password.length() < 4) {
      return bad("密码至少 4 位");
    }
    user.setPassword(passwordEncoder.encode(password));
    userService.updateById(user);
    return ResponseEntity.ok(Map.of("message", "已重置 " + username + " 的密码"));
  }

  @DeleteMapping("/{username}")
  @Transactional
  public ResponseEntity<Map<String, String>> deleteUser(@PathVariable String username) {
    if (PROTECTED_USER.equals(username)) {
      return bad("内置管理员 admin 不可删除");
    }
    if (userService.findByUsername(username) == null) {
      return bad("用户不存在: " + username);
    }
    memberService.lambdaUpdate().eq(GroupMember::getUsername, username).remove();
    userService.removeById(username);
    return ResponseEntity.ok(Map.of("message", "已删除用户 " + username));
  }

  /** Replaces the user's memberships with the given groups; null keeps as-is. */
  private String replaceGroups(String username, List<String> groups) {
    if (groups == null) {
      return null;
    }
    for (String group : groups) {
      if (!groupService.lambdaQuery().eq(PermissionGroup::getName, group).exists()) {
        return "权限组不存在: " + group;
      }
    }
    memberService.lambdaUpdate().eq(GroupMember::getUsername, username).remove();
    for (String group : groups) {
      memberService.save(new GroupMember(group, username));
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  private static List<String> groups(Map<String, Object> body) {
    Object value = body.get("groups");
    return value instanceof List<?> list ? (List<String>) list : null;
  }

  private static String str(Object value) {
    return value == null ? "" : value.toString();
  }

  private ResponseEntity<Map<String, String>> bad(String message) {
    return ResponseEntity.badRequest().body(Map.of("message", message));
  }
}

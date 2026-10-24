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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Permission-group administration. Only members of the ADMIN group may use
 * these endpoints; built-in groups ADMIN/USER cannot be deleted, and the
 * bootstrap admin cannot be removed from ADMIN (lockout protection).
 */
@RestController
@RequestMapping("/api/admin/groups")
@PreAuthorize("hasRole('ADMIN')")
public class AdminGroupController {

  private static final List<String> BUILT_IN_GROUPS = List.of("ADMIN", "USER");

  private final PermissionGroupService groupService;
  private final GroupMemberService memberService;
  private final AppUserService userService;

  public AdminGroupController(PermissionGroupService groupService,
      GroupMemberService memberService, AppUserService userService) {
    this.groupService = groupService;
    this.memberService = memberService;
    this.userService = userService;
  }

  /** All groups with their members. */
  @GetMapping
  public List<Map<String, Object>> listGroups() {
    return groupService.list().stream().map(group -> {
      Map<String, Object> item = new LinkedHashMap<>();
      item.put("name", group.getName());
      item.put("description", group.getDescription());
      item.put("builtIn", BUILT_IN_GROUPS.contains(group.getName()));
      List<String> members = memberService.lambdaQuery()
          .eq(GroupMember::getGroupName, group.getName())
          .list().stream().map(GroupMember::getUsername).toList();
      item.put("members", members);
      return item;
    }).toList();
  }

  /** User list moved to AdminUserController: GET /api/admin/users. */
  @PostMapping
  public ResponseEntity<Map<String, String>> createGroup(@RequestBody Map<String, String> body) {
    String name = body.getOrDefault("name", "").trim().toUpperCase();
    String description = body.getOrDefault("description", "").trim();
    if (!name.matches("^[A-Z][A-Z0-9_]{1,29}$")) {
      return bad("组名需为 2-30 位大写字母/数字/下划线，且以字母开头");
    }
    if (groupService.lambdaQuery().eq(PermissionGroup::getName, name).exists()) {
      return bad("权限组已存在: " + name);
    }
    groupService.save(new PermissionGroup(name, description));
    return ResponseEntity.ok(Map.of("message", "已创建权限组 " + name));
  }

  @DeleteMapping("/{name}")
  @Transactional
  public ResponseEntity<Map<String, String>> deleteGroup(@PathVariable String name) {
    if (BUILT_IN_GROUPS.contains(name)) {
      return bad("内置权限组（ADMIN / USER）不可删除");
    }
    if (!groupService.lambdaQuery().eq(PermissionGroup::getName, name).exists()) {
      return bad("权限组不存在: " + name);
    }
    memberService.lambdaUpdate().eq(GroupMember::getGroupName, name).remove();
    groupService.removeById(name);
    return ResponseEntity.ok(Map.of("message", "已删除权限组 " + name));
  }

  @PostMapping("/{name}/members")
  public ResponseEntity<Map<String, String>> addMember(@PathVariable String name,
      @RequestBody Map<String, String> body) {
    String username = body.getOrDefault("username", "").trim();
    if (!groupService.lambdaQuery().eq(PermissionGroup::getName, name).exists()) {
      return bad("权限组不存在: " + name);
    }
    AppUser user = userService.findByUsername(username);
    if (user == null) {
      return bad("用户不存在: " + username);
    }
    if (memberService.membershipExists(name, username)) {
      return bad("用户 " + username + " 已在该组中");
    }
    memberService.save(new GroupMember(name, username));
    return ResponseEntity.ok(Map.of("message", "已将 " + username + " 加入 " + name));
  }

  @DeleteMapping("/{name}/members/{username}")
  public ResponseEntity<Map<String, String>> removeMember(@PathVariable String name,
      @PathVariable String username) {
    if ("ADMIN".equals(name) && "admin".equals(username)) {
      return bad("内置管理员 admin 不可移出 ADMIN 组，防止失锁");
    }
    if (!memberService.membershipExists(name, username)) {
      return bad("该用户不在此组中");
    }
    memberService.lambdaUpdate()
        .eq(GroupMember::getGroupName, name)
        .eq(GroupMember::getUsername, username)
        .remove();
    return ResponseEntity.ok(Map.of("message", "已将 " + username + " 移出 " + name));
  }

  private ResponseEntity<Map<String, String>> bad(String message) {
    return ResponseEntity.badRequest().body(Map.of("message", message));
  }
}

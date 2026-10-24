package com.demo.ldap.controller;

import com.demo.ldap.service.GroupAdminService;
import com.demo.ldap.service.LdapExportService;
import com.demo.ldap.service.UserAdminService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

/**
 * Administration (ROLE_ADMIN only): permission group management, user
 * management (create / reset password / delete), persist the directory to LDIF.
 */
@Controller
@RequestMapping("/admin")
public class AdminController {

  private final GroupAdminService groupAdminService;
  private final LdapExportService ldapExportService;
  private final UserAdminService userAdminService;

  public AdminController(GroupAdminService groupAdminService, LdapExportService ldapExportService,
      UserAdminService userAdminService) {
    this.groupAdminService = groupAdminService;
    this.ldapExportService = ldapExportService;
    this.userAdminService = userAdminService;
  }

  @GetMapping("/groups")
  public String groups(Model model) {
    model.addAttribute("users", groupAdminService.listUsersWithRoles());
    model.addAttribute("groups", groupAdminService.listGroups());
    model.addAttribute("allUsers", userAdminService.listUsers());
    return "admin/groups";
  }

  /* ---------- 自定义权限组 ---------- */

  @PostMapping("/groups/create")
  public String createGroup(@RequestParam String cn, Principal principal, RedirectAttributes ra) {
    try {
      groupAdminService.createGroup(cn, principal.getName());
      ra.addFlashAttribute("ok", "已创建权限组：" + cn);
    } catch (IllegalStateException e) {
      ra.addFlashAttribute("err", e.getMessage());
    } catch (Exception e) {
      ra.addFlashAttribute("err", "创建失败：" + e.getMessage());
    }
    return "redirect:/admin/groups";
  }

  @PostMapping("/groups/delete")
  public String deleteGroup(@RequestParam String cn, RedirectAttributes ra) {
    try {
      groupAdminService.deleteGroup(cn);
      ra.addFlashAttribute("ok", "已删除权限组：" + cn);
    } catch (IllegalStateException e) {
      ra.addFlashAttribute("err", e.getMessage());
    } catch (Exception e) {
      ra.addFlashAttribute("err", "删除失败：" + e.getMessage());
    }
    return "redirect:/admin/groups";
  }

  @PostMapping("/groups/add-member")
  public String addMember(@RequestParam String cn, @RequestParam String username, RedirectAttributes ra) {
    try {
      groupAdminService.addMember(cn, username);
      ra.addFlashAttribute("ok", "已把 " + username + " 加入组 " + cn);
    } catch (IllegalStateException e) {
      ra.addFlashAttribute("err", e.getMessage());
    } catch (Exception e) {
      ra.addFlashAttribute("err", "操作失败：" + e.getMessage());
    }
    return "redirect:/admin/groups";
  }

  @PostMapping("/groups/remove-member")
  public String removeMember(@RequestParam String cn, @RequestParam String username,
      Principal principal, RedirectAttributes ra) {
    if ("admin".equals(cn) && username.equals(principal.getName())) {
      ra.addFlashAttribute("err", "不能把自己移出 admin 组");
      return "redirect:/admin/groups";
    }
    try {
      groupAdminService.removeMember(cn, username);
      ra.addFlashAttribute("ok", "已把 " + username + " 移出组 " + cn);
    } catch (IllegalStateException e) {
      ra.addFlashAttribute("err", e.getMessage());
    } catch (Exception e) {
      ra.addFlashAttribute("err", "操作失败：" + e.getMessage());
    }
    return "redirect:/admin/groups";
  }

  @PostMapping("/groups/grant")
  public String grant(@RequestParam String username, RedirectAttributes ra) {
    try {
      groupAdminService.setAdmin(username, true);
      ra.addFlashAttribute("ok", "已授予管理员权限：" + username);
    } catch (IllegalStateException e) {
      ra.addFlashAttribute("err", e.getMessage());
    }
    return "redirect:/admin/groups";
  }

  @PostMapping("/groups/revoke")
  public String revoke(@RequestParam String username, Principal principal, RedirectAttributes ra) {
    if (username.equals(principal.getName())) {
      ra.addFlashAttribute("err", "不能收回自己的管理员权限");
      return "redirect:/admin/groups";
    }
    try {
      groupAdminService.setAdmin(username, false);
      ra.addFlashAttribute("ok", "已收回管理员权限：" + username);
    } catch (IllegalStateException e) {
      ra.addFlashAttribute("err", e.getMessage());
    }
    return "redirect:/admin/groups";
  }

  /**
   * Persist the current directory (users + group memberships) to ldap.ldif files.
   */
  @PostMapping("/export-ldif")
  public String exportLdif(RedirectAttributes ra) {
    try {
      ldapExportService.exportToFile();
      ra.addFlashAttribute("ok", "已固化到 data/ldap.ldif 和 src/main/resources/ldap.ldif，重启后依然生效");
    } catch (Exception e) {
      ra.addFlashAttribute("err", "固化失败：" + e.getMessage());
    }
    return "redirect:/admin/groups";
  }

  /* ---------- 用户管理 ---------- */

  @GetMapping("/users")
  public String users(Model model) {
    model.addAttribute("users", userAdminService.listUsers());
    return "admin/users";
  }

  @PostMapping("/users/create")
  public String createUser(@RequestParam String username, @RequestParam String password,
      @RequestParam(required = false) String firstName, @RequestParam(required = false) String lastName,
      @RequestParam(required = false) String email, RedirectAttributes ra) {
    username = username == null ? "" : username.trim();
    if (username.isEmpty() || password == null || password.length() < 4) {
      ra.addFlashAttribute("err", "用户名不能为空，密码至少 4 位");
      return "redirect:/admin/users";
    }
    try {
      userAdminService.createUser(username, password, firstName, lastName, email);
      ra.addFlashAttribute("ok", "已创建用户：" + username);
    } catch (IllegalStateException e) {
      ra.addFlashAttribute("err", e.getMessage());
    } catch (Exception e) {
      ra.addFlashAttribute("err", "创建失败：" + e.getMessage());
    }
    return "redirect:/admin/users";
  }

  @PostMapping("/users/reset-password")
  public String resetPassword(@RequestParam String username, @RequestParam String password,
      RedirectAttributes ra) {
    if (password == null || password.length() < 4) {
      ra.addFlashAttribute("err", "新密码至少 4 位");
      return "redirect:/admin/users";
    }
    try {
      userAdminService.resetPassword(username, password);
      ra.addFlashAttribute("ok", "已重置密码：" + username);
    } catch (IllegalStateException e) {
      ra.addFlashAttribute("err", e.getMessage());
    } catch (Exception e) {
      ra.addFlashAttribute("err", "重置失败：" + e.getMessage());
    }
    return "redirect:/admin/users";
  }

  @PostMapping("/users/update")
  public String updateUser(@RequestParam String username,
      @RequestParam(required = false) String newUsername,
      @RequestParam(required = false) String firstName,
      @RequestParam(required = false) String lastName,
      @RequestParam(required = false) String email,
      Principal principal, RedirectAttributes ra) {
    if (username.equals(principal.getName()) && newUsername != null && !newUsername.isBlank()
        && !newUsername.trim().equals(username)) {
      ra.addFlashAttribute("err", "不能修改自己的用户名，请让其他管理员操作");
      return "redirect:/admin/users";
    }
    try {
      userAdminService.updateUser(username, newUsername, firstName, lastName, email);
      ra.addFlashAttribute("ok", "已更新用户：" + username);
    } catch (IllegalStateException e) {
      ra.addFlashAttribute("err", e.getMessage());
    } catch (Exception e) {
      ra.addFlashAttribute("err", "更新失败：" + e.getMessage());
    }
    return "redirect:/admin/users";
  }

  @PostMapping("/users/delete")
  public String deleteUser(@RequestParam String username, Principal principal, RedirectAttributes ra) {
    if (username.equals(principal.getName())) {
      ra.addFlashAttribute("err", "不能删除当前登录的管理员账号");
      return "redirect:/admin/users";
    }
    try {
      userAdminService.deleteUser(username);
      ra.addFlashAttribute("ok", "已删除用户：" + username);
    } catch (IllegalStateException e) {
      ra.addFlashAttribute("err", e.getMessage());
    } catch (Exception e) {
      ra.addFlashAttribute("err", "删除失败：" + e.getMessage());
    }
    return "redirect:/admin/users";
  }

}

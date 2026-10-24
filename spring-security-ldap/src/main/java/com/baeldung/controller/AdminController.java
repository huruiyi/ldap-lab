package com.baeldung.controller;

import com.baeldung.ldap.service.GroupAdminService;
import com.baeldung.ldap.service.LdapExportService;
import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 权限组管理（仅 ROLE_ADMIN）：查看用户的组成员关系，授予/收回管理员权限，固化到 LDIF。
 */
@Controller
@RequestMapping("/admin")
public class AdminController {

  private final GroupAdminService groupAdminService;
  private final LdapExportService ldapExportService;

  public AdminController(GroupAdminService groupAdminService, LdapExportService ldapExportService) {
    this.groupAdminService = groupAdminService;
    this.ldapExportService = ldapExportService;
  }

  @GetMapping("/groups")
  public String groups(Model model) {
    model.addAttribute("users", groupAdminService.listUsersWithRoles());
    return "admin/groups";
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

  /** 把当前 LDAP 目录内容（用户 + 组成员关系）固化回 src/main/resources/users.ldif */
  @PostMapping("/export-ldif")
  public String exportLdif(RedirectAttributes ra) {
    try {
      ldapExportService.exportToFile();
      ra.addFlashAttribute("ok", "已固化到 src/main/resources/users.ldif，重启后依然生效");
    } catch (Exception e) {
      ra.addFlashAttribute("err", "固化失败：" + e.getMessage());
    }
    return "redirect:/admin/groups";
  }
}

package com.baeldung.controller;

import com.baeldung.ldap.service.RegistrationService;
import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 修改密码：验证旧密码（LDAP bind 比对）后更新目录中的 userPassword。
 */
@Controller
public class PasswordController {

  private final RegistrationService registrationService;

  public PasswordController(RegistrationService registrationService) {
    this.registrationService = registrationService;
  }

  @GetMapping("/password")
  public String passwordPage() {
    return "password";
  }

  @PostMapping("/password")
  public String changePassword(@RequestParam String oldPassword,
                               @RequestParam String newPassword,
                               @RequestParam String confirmPassword,
                               Principal principal, Model model) {
    if (newPassword == null || newPassword.length() < 4) {
      model.addAttribute("err", "新密码至少 4 位");
      return "password";
    }
    if (!newPassword.equals(confirmPassword)) {
      model.addAttribute("err", "两次输入的新密码不一致");
      return "password";
    }
    if (!registrationService.verifyPassword(principal.getName(), oldPassword)) {
      model.addAttribute("err", "原密码不正确");
      return "password";
    }
    registrationService.changePassword(principal.getName(), newPassword);
    model.addAttribute("ok", "密码已修改，下次登录请使用新密码");
    return "password";
  }
}

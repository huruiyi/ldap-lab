package com.baeldung.controller;

import com.baeldung.ldap.service.RegistrationService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 用户注册：写入内嵌 LDAP 目录，成功后即可用新账号登录。
 */
@Controller
public class RegistrationController {

  private final RegistrationService registrationService;

  public RegistrationController(RegistrationService registrationService) {
    this.registrationService = registrationService;
  }

  @GetMapping("/register")
  public String registerPage() {
    return "register";
  }

  @PostMapping("/register")
  public String doRegister(@RequestParam String username,
                           @RequestParam String password,
                           @RequestParam String confirmPassword,
                           @RequestParam(required = false) String fullName,
                           @RequestParam(required = false) String lastName,
                           @RequestParam(required = false) String email,
                           Model model) {
    username = username == null ? "" : username.trim();
    if (username.isEmpty()) {
      model.addAttribute("err", "用户名不能为空");
      return "register";
    }
    if (password == null || password.length() < 4) {
      model.addAttribute("err", "密码至少 4 位");
      return "register";
    }
    if (!password.equals(confirmPassword)) {
      model.addAttribute("err", "两次输入的密码不一致");
      return "register";
    }
    try {
      registrationService.register(username, password, fullName, lastName, email);
    } catch (IllegalStateException e) {
      model.addAttribute("err", e.getMessage());
      return "register";
    } catch (Exception e) {
      model.addAttribute("err", "注册失败：" + e.getMessage());
      return "register";
    }
    return "redirect:/login?registered";
  }
}

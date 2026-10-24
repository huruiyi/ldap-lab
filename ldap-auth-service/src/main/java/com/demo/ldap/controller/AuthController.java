package com.demo.ldap.controller;

import java.util.Map;

import com.demo.ldap.service.UserProfileService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Web controller for the login page, the user home page and the 403 page.
 */
@Controller
public class AuthController {

  private final UserProfileService userProfileService;

  public AuthController(UserProfileService userProfileService) {
    this.userProfileService = userProfileService;
  }

  @GetMapping("/login")
  public String login() {
    return "login";
  }

  @GetMapping({"/", "/home"})
  public String home(Authentication authentication, Model model) {
    String username = authentication.getName();
    model.addAttribute("username", username);
    model.addAttribute("authorities", authentication.getAuthorities());
    boolean isAdmin = authentication.getAuthorities().stream()
        .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    model.addAttribute("isAdmin", isAdmin);
    // profile attributes from the LDAP directory (givenName / sn / mail)
    Map<String, String> profile = userProfileService.getProfile(username);
    model.addAttribute("firstName", profile.getOrDefault("givenName", ""));
    model.addAttribute("lastName", profile.getOrDefault("sn", ""));
    model.addAttribute("email", profile.getOrDefault("mail", ""));
    return "home";
  }

  @GetMapping("/403")
  public String forbidden(Authentication authentication, Model model) {
    if (authentication != null) {
      model.addAttribute("username", authentication.getName());
    }
    return "error/403";
  }

}
